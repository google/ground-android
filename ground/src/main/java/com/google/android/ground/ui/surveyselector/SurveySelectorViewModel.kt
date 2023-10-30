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
import com.google.android.ground.model.Survey
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

/** Represents view state and behaviors of the survey selector dialog. */
@OptIn(FlowPreview::class)
class SurveySelectorViewModel
@Inject
internal constructor(
  private val surveyRepository: SurveyRepository,
  private val authManager: AuthenticationManager,
  private val navigator: Navigator,
  private val activateSurveyUseCase: ActivateSurveyUseCase,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val userRepository: UserRepository
) : AbstractViewModel() {

  val surveyListState: MutableStateFlow<State?> = MutableStateFlow(null)
  val surveySummaries: Flow<List<SurveyItem>>

  private var shouldListenToRemoteChanges = true
  private var listenerRegistration: ListenerRegistration? = null

  init {
    surveySummaries =
      createSurveySummaries().onEach {
        if (it.isEmpty()) {
          setNotFound()
        } else {
          setLoaded()
        }
      }
  }

  /** Returns a flow of locally stored surveys. */
  private fun offlineSurveys(): Flow<List<Survey>> =
    surveyRepository.offlineSurveys.onEach { setLoading() }

  /** Returns a flow of remotely stored surveys. */
  private suspend fun allSurveys(): Flow<List<Survey>> =
    surveyRepository
      .getSurveySummaries(authManager.currentUser) { listenerRegistration = it }
      .onEach { setLoading() }
      .takeWhile { shouldListenToRemoteChanges }

  /** Returns a flow of [SurveyItem] to be displayed to the user. */
  private fun createSurveySummaries(): Flow<List<SurveyItem>> =
    offlineSurveys().flatMapMerge { offlineSurveys: List<Survey> ->
      allSurveys().map { allSurveys: List<Survey> ->
        allSurveys
          .map { createSurveyItem(it, offlineSurveys) }
          .sortedBy { it.surveyTitle }
          .sortedByDescending { it.isAvailableOffline }
      }
    }

  private fun createSurveyItem(survey: Survey, localSurveys: List<Survey>): SurveyItem =
    SurveyItem(
      surveyId = survey.id,
      surveyTitle = survey.title,
      surveyDescription = survey.description,
      isAvailableOffline = localSurveys.any { it.id == survey.id }
    )

  /** Triggers the specified survey to be loaded and activated. */
  fun activateSurvey(surveyId: String) =
    viewModelScope.launch {
      // TODO(#1497): Handle exceptions thrown by activateSurvey().
      setLoading()

      // Disable listening to remote changes to prevent multiple progress loading spinners
      shouldListenToRemoteChanges = false
      listenerRegistration?.remove()

      activateSurveyUseCase(surveyId)
      setLoaded()
      navigateToHomeScreen()
    }

  private fun navigateToHomeScreen() {
    navigator.navigate(HomeScreenFragmentDirections.showHomeScreen())
  }

  fun deleteSurvey(surveyId: String) {
    externalScope.launch(ioDispatcher) { surveyRepository.removeOfflineSurvey(surveyId) }
  }

  fun signOut() {
    userRepository.signOut()
  }

  private fun setNotFound() {
    surveyListState.value = State.NOT_FOUND
  }

  private fun setLoading() {
    surveyListState.value = State.LOADING
  }

  private fun setLoaded() {
    surveyListState.value = State.LOADED
  }

  enum class State {
    NOT_FOUND,
    LOADING,
    LOADED,
  }
}
