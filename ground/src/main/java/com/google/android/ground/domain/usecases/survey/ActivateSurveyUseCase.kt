/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.domain.usecases.survey

import com.google.android.ground.model.Survey
import com.google.android.ground.repository.SurveyRepository
import javax.inject.Inject
import timber.log.Timber

class ActivateSurveyUseCase
@Inject
constructor(
  private val surveyRepository: SurveyRepository,
  private val makeSurveyAvailableOffline: MakeSurveyAvailableOfflineUseCase,
  private val syncSurvey: SyncSurveyUseCase,
) {
  suspend operator fun invoke(surveyId: String) {
    // Do nothing if specified survey is already active.
    if (surveyId == surveyRepository.activeSurvey?.id) {
      return
    }

    val survey = surveyRepository.getOfflineSurvey(surveyId)
    surveyRepository.activeSurvey =
      if (survey == null) {
        // Survey not loaded? Fetch and store locally.
        makeSurveyAvailableOffline(surveyId)
      } else {
        // Survey loaded? Try to fetch from remote, but don't fail if we're offline.
        tryToSyncSurveyWithRemote(survey)
      }
  }

  private suspend fun tryToSyncSurveyWithRemote(survey: Survey): Survey =
    try {
      syncSurvey(survey.id)
    } catch (e: Throwable) {
      Timber.d(e, "Failed to sync survey. This is expected if the app is online")
      survey
    }
}
