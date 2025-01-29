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
package com.google.android.ground.domain.usecases.survey

import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.repository.SurveyRepository
import javax.inject.Inject

class RemoveOfflineSurveyUseCase
@Inject
constructor(
  private val localSurveyStore: LocalSurveyStore,
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

    val survey = localSurveyStore.getSurveyById(surveyId) ?: return
    localSurveyStore.deleteSurvey(survey)
  }
}
