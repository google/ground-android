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

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.viewModelScope
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.Survey
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import io.reactivex.Flowable
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Represents view state and behaviors of the survey selector dialog. */
class SurveySelectorViewModel
@Inject
internal constructor(
  private val surveyRepository: SurveyRepository,
  private val authManager: AuthenticationManager,
  private val navigator: Navigator,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AbstractViewModel() {

  val surveySummaries: LiveData<List<SurveyItem>>

  init {
    surveySummaries =
      LiveDataReactiveStreams.fromPublisher(
        offlineSurveys.flatMap { offlineSurveys: List<Survey> ->
          allSurveys.map { allSurveys: List<Survey> ->
            allSurveys.map { createSurveyItem(it, offlineSurveys) }
          }
        }
      )
  }

  private fun createSurveyItem(survey: Survey, localSurveys: List<Survey>): SurveyItem =
    SurveyItem(
      surveyId = survey.id,
      surveyTitle = survey.title,
      surveyDescription = survey.description.ifEmpty { "Description Missing" },
      isAvailableOffline = localSurveys.contains(survey)
    )

  private val offlineSurveys: Flowable<List<Survey>>
    get() = surveyRepository.offlineSurveys

  private val allSurveys: Flowable<List<Survey>>
    get() = surveyRepository.getSurveySummaries(authManager.currentUser).toFlowable()

  /** Triggers the specified survey to be loaded and activated. */
  fun activateSurvey(surveyId: String) =
    viewModelScope.launch {
      // TODO(#1497): Handle exceptions thrown by activateSurvey().
      surveyRepository.activateSurvey(surveyId)
      navigateToHomeScreen()
    }

  private fun navigateToHomeScreen() {
    navigator.navigate(HomeScreenFragmentDirections.showHomeScreen())
  }

  fun deleteSurvey(surveyId: String) {
    externalScope.launch(ioDispatcher) { surveyRepository.deleteSurvey(surveyId) }
  }
}
