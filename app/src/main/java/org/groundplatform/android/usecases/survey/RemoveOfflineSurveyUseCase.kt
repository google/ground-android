/*
 * Copyright 2025 Google LLC
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

class RemoveOfflineSurveyUseCase
@Inject
constructor(
  private val localValueStore: LocalValueStore,
  private val surveyRepository: SurveyRepository,
) {

  /**
   * Attempts to remove the locally synced survey. Also, deactivates it before removing, if it is
   * active. Doesn't throw an error if it doesn't exist.
   */
  suspend operator fun invoke(surveyId: String) {
    if (surveyRepository.isSurveyActive(surveyId)) {
      surveyRepository.clearActiveSurvey()
    }

    localValueStore.clearLastCameraPosition(surveyId)

    surveyRepository.removeOfflineSurvey(surveyId)
  }
}
