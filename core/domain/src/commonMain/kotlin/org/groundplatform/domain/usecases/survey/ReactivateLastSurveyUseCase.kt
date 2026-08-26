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

package org.groundplatform.domain.usecases.survey

import org.groundplatform.domain.repository.SurveyRepositoryInterface

/**
 * Attempts to reactivate the last survey. If a survey is already active, does nothing.
 *
 * Returns `true` if a survey is or was activated, `false` if there is no previous survey to
 * reactivate.
 */
class ReactivateLastSurveyUseCase(
  private val activateSurvey: ActivateSurveyUseCase,
  private val surveyRepository: SurveyRepositoryInterface,
) {

  suspend operator fun invoke(): Boolean {
    if (surveyRepository.activeSurvey != null) {
      // Skip if there is an active survey.
      return true
    }
    val lastActiveSurveyId = surveyRepository.lastActiveSurveyId
    if (lastActiveSurveyId.isEmpty()) {
      // Nothing to be re-activated.
      return false
    }
    return activateSurvey(lastActiveSurveyId)
  }
}
