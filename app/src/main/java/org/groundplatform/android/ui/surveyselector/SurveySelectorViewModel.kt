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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.groundplatform.android.coroutines.ApplicationScope
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.android.usecases.survey.ListAvailableSurveysUseCase
import org.groundplatform.android.usecases.survey.RemoveOfflineSurveyUseCase
import timber.log.Timber

/** Represents view state and behaviors of the survey selector dialog. */
class SurveySelectorViewModel
@Inject
internal constructor(
  private val activateSurveyUseCase: ActivateSurveyUseCase,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val listAvailableSurveysUseCase: ListAvailableSurveysUseCase,
  private val removeOfflineSurveyUseCase: RemoveOfflineSurveyUseCase,
  private val userRepository: UserRepository,
) : AbstractViewModel() {

  private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.FetchingSurveys)
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  private val _surveys = MutableStateFlow<List<SurveyListItem>>(emptyList())

  private val _onDeviceSurveys = MutableStateFlow<List<SurveyListItem>>(emptyList())
  val onDeviceSurveys: StateFlow<List<SurveyListItem>> = _onDeviceSurveys

  private val _sharedWithSurveys = MutableStateFlow<List<SurveyListItem>>(emptyList())
  val sharedWithSurveys: StateFlow<List<SurveyListItem>> = _sharedWithSurveys

  private val _publicListSurveys = MutableStateFlow<List<SurveyListItem>>(emptyList())
  val publicListSurveys: StateFlow<List<SurveyListItem>> = _publicListSurveys

  val showDeleteDialog = MutableStateFlow(false)
  val selectedSurveyId = mutableStateOf<String?>(null)

  var surveyActivationInProgress = false

  init {
    viewModelScope.launch {
      getSurveyList().distinctUntilChanged().collect {
        _uiState.emit(UiState.SurveyListAvailable(it))
      }
    }
  }

  /** Returns a flow of [SurveyListItem] to be displayed to the user. */
  private fun getSurveyList(): Flow<List<SurveyListItem>> =
    listAvailableSurveysUseCase().map { surveys ->
      surveys.sortedWith(compareBy({ !it.availableOffline }, { it.title }))
    }

  /** Triggers the specified survey to be loaded and activated. */
  fun activateSurvey(surveyId: String) {
    synchronized(this) {
      if (surveyActivationInProgress) {
        // Ignore extra clicks while survey is loading, see #2729.
        Timber.v("Ignoring extra survey click.")
        return
      }
      surveyActivationInProgress = true
    }
    viewModelScope.launch {
      runCatching {
          _uiState.emit(UiState.ActivatingSurvey)
          activateSurveyUseCase(surveyId)
        }
        .fold(
          onSuccess = { result ->
            if (result) {
              onSurveyActivated()
            } else {
              onSurveyActivationFailed()
            }
          },
          onFailure = { onSurveyActivationFailed(it) },
        )
    }
  }

  private suspend fun onSurveyActivated() {
    surveyActivationInProgress = false
    _uiState.emit(UiState.SurveyActivated)
    _uiState.emit(UiState.NavigateToHome)
  }

  private suspend fun onSurveyActivationFailed(error: Throwable? = null) {
    Timber.e(error, "Failed to activate survey")
    surveyActivationInProgress = false
    _uiState.emit(UiState.Error(error))
  }

  fun signOut() {
    userRepository.signOut()
  }

  fun setSurveys(surveys: List<SurveyListItem>) {
    _surveys.value = surveys

    val grouped =
      surveys.groupBy {
        when {
          it.availableOffline -> "onDevice"
          it.generalAccess == Survey.GeneralAccess.PUBLIC -> "public"
          it.generalAccess == Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED ||
            it.generalAccess == Survey.GeneralAccess.RESTRICTED ||
            it.generalAccess == Survey.GeneralAccess.UNLISTED -> "sharedWith"
          else -> "other"
        }
      }
    _onDeviceSurveys.value = grouped["onDevice"].orEmpty()
    _sharedWithSurveys.value = grouped["sharedWith"].orEmpty()
    _publicListSurveys.value = grouped["public"].orEmpty()
  }

  fun openDeleteDialog(id: String) {
    selectedSurveyId.value = id
    showDeleteDialog.value = true
  }

  fun closeDeleteDialog() {
    showDeleteDialog.value = false
    selectedSurveyId.value = null
  }

  fun confirmDelete(selectedSurveyId: String) {
    deleteSurvey(selectedSurveyId)
    closeDeleteDialog()
  }

  private fun deleteSurvey(surveyId: String) {
    externalScope.launch(ioDispatcher) { removeOfflineSurveyUseCase(surveyId) }
    _surveys.value = _surveys.value.filterNot { it.id == surveyId }
    setSurveys(_surveys.value)
  }
}
