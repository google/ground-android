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
import com.google.android.ground.model.Survey
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.common.collect.ImmutableList
import io.reactivex.Single
import javax.inject.Inject

/** Represents view state and behaviors of the survey selector dialog. */
class SurveySelectorViewModel
@Inject
internal constructor(
  private val surveyRepository: SurveyRepository,
  private val authManager: AuthenticationManager
) : AbstractViewModel() {

  private var attemptingToActivate: String = ""

  val surveyActivated: LiveData<Boolean>
  val surveySummaries: LiveData<List<SurveyItem>>

  init {
    surveySummaries =
      LiveDataReactiveStreams.fromPublisher(
        offlineSurveys
          .flatMap { offlineSurveys: List<Survey> ->
            allSurveys.map { allSurveys: List<Survey> ->
              allSurveys.map { createSurveyItem(it, offlineSurveys) }
            }
          }
          .toFlowable()
      )
    surveyActivated =
      LiveDataReactiveStreams.fromPublisher(
        surveyRepository.activeSurvey
          .filter { it.isPresent }
          .map { it.get().id == attemptingToActivate }
      )
  }

  private fun createSurveyItem(survey: Survey, localSurveys: List<Survey>): SurveyItem =
    SurveyItem(
      surveyId = survey.id,
      surveyTitle = survey.title,
      surveyDescription = survey.description.ifEmpty { "Description Missing" },
      isAvailableOffline = localSurveys.contains(survey)
    )

  val offlineSurveys: Single<ImmutableList<Survey>>
    get() = surveyRepository.offlineSurveys

  private val allSurveys: Single<List<Survey>>
    get() = surveyRepository.getSurveySummaries(authManager.currentUser)

  fun activateSurvey(surveyId: String) {
    attemptingToActivate = surveyId
    surveyRepository.activateSurvey(surveyId)
  }
}
