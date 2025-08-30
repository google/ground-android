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
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.repository.SurveyRepository

/** Attempts to reactivate the last survey. If survey is already active, does nothing. */
class ReactivateLastSurveyUseCase
@Inject
constructor(
  private val activateSurvey: ActivateSurveyUseCase,
  private val localValueStore: LocalValueStore,
  private val surveyRepository: SurveyRepository,
) {

  suspend operator fun invoke(): Boolean {
    if (surveyRepository.activeSurvey != null) {
      // Skip if there is an active survey.
      return true
    }
    val lastActiveSurveyId = localValueStore.lastActiveSurveyId
    if (lastActiveSurveyId.isEmpty()) {
      // Nothing to be re-activated.
      return false
    }
    return activateSurvey(lastActiveSurveyId)
  }
}
