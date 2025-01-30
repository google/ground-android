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
package com.google.android.ground.ui.surveyselector

import androidx.lifecycle.viewModelScope
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.domain.usecases.survey.ListAvailableSurveysUseCase
import com.google.android.ground.domain.usecases.survey.RemoveOfflineSurveyUseCase
import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.ui.common.AbstractViewModel
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
import timber.log.Timber

/** Represents view state and behaviors of the survey selector dialog. */
class SurveySelectorViewModel
@Inject
internal constructor(
  private val activateSurveyUseCase: ActivateSurveyUseCase,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val listAvailableSurveysUseCase: ListAvailableSurveysUseCase,
  private val remoteOfflineSurveyUseCase: RemoveOfflineSurveyUseCase,
  private val userRepository: UserRepository,
) : AbstractViewModel() {

  private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.FetchingSurveys)
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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
    _uiState.emit(UiState.Error)
  }

  fun deleteSurvey(surveyId: String) {
    externalScope.launch(ioDispatcher) { remoteOfflineSurveyUseCase(surveyId) }
  }

  fun signOut() {
    userRepository.signOut()
  }
}
