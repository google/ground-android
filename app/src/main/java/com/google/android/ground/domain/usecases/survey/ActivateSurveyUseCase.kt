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

import com.google.android.ground.repository.SurveyRepository
import javax.inject.Inject

class ActivateSurveyUseCase
@Inject
constructor(
  private val surveyRepository: SurveyRepository,
  private val makeSurveyAvailableOffline: MakeSurveyAvailableOfflineUseCase,
) {
  /**
   * Sets the survey with the specified ID as the currently active. First attempts to load the
   * survey from the local db, and if not present, fetches from remote and activates offline sync.
   * Throws an error if the survey isn't found or cannot be made available offlien. Activating a
   * survey which is already available offline doesn't force a resync, since this is handled by
   * [com.google.android.ground.persistence.sync.SurveySyncWorker].
   */
  suspend operator fun invoke(surveyId: String) {
    // Do nothing if specified survey is already active.
    if (surveyId == surveyRepository.activeSurvey?.id) {
      return
    }

    surveyRepository.getOfflineSurvey(surveyId)
      ?: makeSurveyAvailableOffline(surveyId)
      ?: error("Survey $surveyId not found in remote db")

    surveyRepository.selectedSurveyId = surveyId
  }
}
