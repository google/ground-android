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
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.android.usecases.survey.ListAvailableSurveysUseCase
import org.groundplatform.android.usecases.survey.RemoveOfflineSurveyUseCase
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
  private val removeOfflineSurveyUseCase: RemoveOfflineSurveyUseCase,
  private val userRepository: UserRepository,
  savedStateHandle: SavedStateHandle,
) : AbstractViewModel() {

  private val surveyIdToActivate: String? = savedStateHandle["surveyId"]

  private val _events = Channel<SurveySelectorEvent>()
  val events = _events.receiveAsFlow()

  private val _isActivating = MutableStateFlow(false)

  private val surveyList: Flow<List<SurveyListItem>> =
    listAvailableSurveysUseCase()
      .map { surveys -> surveys.sortedWith(compareBy({ !it.availableOffline }, { it.title })) }
      .catch { error ->
        Timber.e(error, "Failed to load available surveys")
        _events.send(SurveySelectorEvent.ShowError(error))
        emit(emptyList())
      }

  val uiState: StateFlow<SurveySelectorUiState> =
    combine(surveyList, _isActivating) { surveys, isActivating ->
        SurveySelectorUiState(
          isLoading = isActivating, // Initial loading handled by StateFlow initialValue
          onDeviceSurveys = surveys.filter { it.isOnDevice() },
          sharedSurveys = surveys.filter { it.isShared() },
          publicSurveys = surveys.filter { it.isPublic() },
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SurveySelectorUiState(isLoading = true),
      )

  init {
    if (!surveyIdToActivate.isNullOrBlank()) {
      viewModelScope.launch {
        // Wait for the survey list to contain the target survey
        surveyList.first { surveys -> surveys.any { it.id == surveyIdToActivate } }
        // Once found, activate it
        activateSurvey(surveyIdToActivate)
      }
    }
  }

  /** Triggers the specified survey to be loaded and activated. */
  fun activateSurvey(surveyId: String) {
    if (_isActivating.value) return

    _isActivating.value = true
    viewModelScope.launch {
      runCatching { activateSurveyUseCase(surveyId) }
        .fold(
          onSuccess = { result ->
            _isActivating.value = false
            if (result) {
              _events.send(SurveySelectorEvent.NavigateToHome)
            } else {
              _events.send(SurveySelectorEvent.ShowError(Exception("Survey activation failed")))
            }
          },
          onFailure = {
            Timber.e(it, "Failed to activate survey")
            _isActivating.value = false
            _events.send(SurveySelectorEvent.ShowError(it))
          },
        )
    }
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
}

sealed interface SurveySelectorEvent {
  object NavigateToHome : SurveySelectorEvent

  data class ShowError(val error: Throwable) : SurveySelectorEvent
}
