/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groundplatform.android.ui.surveyselector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.groundplatform.android.di.coroutines.ApplicationScope
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.android.system.GmsQrCodeScanner
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.android.usecases.survey.ListAvailableSurveysUseCase
import org.groundplatform.android.usecases.survey.RemoveOfflineSurveyUseCase
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.domain.usecases.survey.GetSurveyListItemUseCase
import org.groundplatform.domain.util.SurveyQrCodeParser
import timber.log.Timber

/** Represents view state and behaviors of the survey selector dialog. */
@HiltViewModel
class SurveySelectorViewModel
@Inject
internal constructor(
  private val activateSurveyUseCase: ActivateSurveyUseCase,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  listAvailableSurveysUseCase: ListAvailableSurveysUseCase,
  private val gmsQrCodeScanner: GmsQrCodeScanner,
  private val surveyQrCodeParser: SurveyQrCodeParser,
  private val removeOfflineSurveyUseCase: RemoveOfflineSurveyUseCase,
  private val getSurveyListItem: GetSurveyListItemUseCase,
  private val userRepository: UserRepositoryInterface,
  savedStateHandle: SavedStateHandle,
) : AbstractViewModel() {

  private val surveyIdToActivate: String? = savedStateHandle["surveyId"]

  private val _events = Channel<SurveySelectorEvent>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  private val _isLoadingSurvey = MutableStateFlow(false)

  private val _pendingJoinSurvey = MutableStateFlow<SurveyListItem?>(null)

  private val surveyList: Flow<List<SurveyListItem>> =
    listAvailableSurveysUseCase()
      .map { surveys -> surveys.sortedWith(compareBy({ !it.availableOffline }, { it.title })) }
      .catch { error ->
        Timber.e(error, "Failed to load available surveys")
        _events.send(SurveySelectorEvent.ShowError(error.toSurveySelectorError()))
        emit(emptyList())
      }

  val uiState: StateFlow<SurveySelectorUiState> =
    combine(surveyList, _isLoadingSurvey, _pendingJoinSurvey) { surveys, isLoading, pending ->
        SurveySelectorUiState(
          isLoading = isLoading, // Initial loading handled by StateFlow initialValue
          onDeviceSurveys = surveys.filter { it.isOnDevice() },
          sharedSurveys = surveys.filter { it.isShared() },
          publicSurveys = surveys.filter { it.isPublic() },
          pendingJoinSurvey = pending,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SurveySelectorUiState(isLoading = true),
      )

  init {
    if (!surveyIdToActivate.isNullOrBlank()) {
      viewModelScope.launch { activateSurvey(surveyIdToActivate) }
    }
  }

  /** Triggers the specified survey to be loaded and activated. */
  fun activateSurvey(surveyId: String) {
    if (_isLoadingSurvey.value) return

    _isLoadingSurvey.value = true
    viewModelScope.launch {
      runCatching { activateSurveyUseCase(surveyId) }
        .fold(
          onSuccess = { result ->
            _isLoadingSurvey.value = false
            if (result) {
              _events.send(SurveySelectorEvent.NavigateToHome)
            } else {
              _events.send(
                SurveySelectorEvent.ShowError(
                  Exception("Survey activation failed").toSurveySelectorError()
                )
              )
            }
          },
          onFailure = {
            Timber.e(it, "Failed to activate survey")
            _isLoadingSurvey.value = false
            _events.send(SurveySelectorEvent.ShowError(it.toSurveySelectorError()))
          },
        )
    }
  }

  fun joinSurveyByQrCode() {
    viewModelScope.launch {
      when (val result = gmsQrCodeScanner.scan()) {
        is GmsQrCodeScanner.Result.Success -> {
          val surveyId = surveyQrCodeParser(result.text)
          if (surveyId == null) {
            _events.send(SurveySelectorEvent.ShowError(SurveySelectorEvent.ErrorType.InvalidQrCode))
          } else {
            requestJoinSurveyConfirmation(surveyId)
          }
        }
        is GmsQrCodeScanner.Result.Cancelled -> {
          /* Nothing to do */
        }
        is GmsQrCodeScanner.Result.Error -> {
          _events.send(SurveySelectorEvent.ShowError(result.cause.toSurveySelectorError()))
        }
      }
    }
  }

  private suspend fun requestJoinSurveyConfirmation(surveyId: String) {
    _isLoadingSurvey.value = true
    val item =
      surveyList.first().firstOrNull { it.id == surveyId }
        ?: runCatching { getSurveyListItem(surveyId) }
          .onFailure { Timber.e(it, "Failed to load survey $surveyId for confirmation") }
          .getOrNull()
    _isLoadingSurvey.value = false
    if (item == null) {
      _events.send(SurveySelectorEvent.ShowError(SurveySelectorEvent.ErrorType.InvalidQrCode))
    } else {
      _pendingJoinSurvey.value = item
    }
  }

  fun confirmJoinSurvey() {
    _pendingJoinSurvey.value?.let { pending ->
      _pendingJoinSurvey.value = null
      activateSurvey(pending.id)
    }
  }

  fun dismissJoinSurveyConfirmation() {
    _pendingJoinSurvey.value = null
  }

  /** Signs out the current user. */
  fun signOut() {
    userRepository.signOut()
  }

  /**
   * Confirms the deletion of a local survey.
   *
   * @param surveyId The ID of the survey to delete.
   */
  fun confirmDelete(surveyId: String) {
    externalScope.launch(ioDispatcher) { removeOfflineSurveyUseCase(surveyId) }
  }

  private fun Throwable.toSurveySelectorError(): SurveySelectorEvent.ErrorType =
    if (this is TimeoutCancellationException) SurveySelectorEvent.ErrorType.Timeout
    else SurveySelectorEvent.ErrorType.Generic(this)
}
