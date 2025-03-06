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

package org.groundplatform.android.usecases.survey

import javax.inject.Inject
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.sync.SurveySyncWorker
import org.groundplatform.android.repository.SurveyRepository

/**
 * Sets the survey with the specified ID as the currently active.
 *
 * First attempts to load the survey from the local db. If not present, fetches from remote and
 * activates offline sync. Throws an error if the survey isn't found or cannot be made available
 * offline. Activating a survey which is already available offline doesn't force a re-sync, since
 * this is handled by [SurveySyncWorker].
 */
class ActivateSurveyUseCase
@Inject
constructor(
  private val localSurveyStore: LocalSurveyStore,
  private val makeSurveyAvailableOffline: MakeSurveyAvailableOfflineUseCase,
  private val surveyRepository: SurveyRepository,
) {

  /**
   * @return `true` if the survey was successfully activated or was already active, otherwise false.
   */
  suspend operator fun invoke(surveyId: String): Boolean {
    if (surveyRepository.isSurveyActive(surveyId)) {
      // Do nothing if survey is already active.
      return true
    }

    localSurveyStore.getSurveyById(surveyId)
      ?: makeSurveyAvailableOffline(surveyId)
      ?: error("Survey $surveyId not found in remote db")

    surveyRepository.activateSurvey(surveyId)

    return surveyRepository.isSurveyActive(surveyId)
  }
}
