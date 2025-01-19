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
import com.google.android.ground.repository.LocalSurveyRepository
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.RemoteSurveyRepository
import javax.inject.Inject

class SyncSurveyUseCase
@Inject
constructor(
  private val localSurveyRepository: LocalSurveyRepository,
  private val loiRepository: LocationOfInterestRepository,
  private val remoteSurveyRepository: RemoteSurveyRepository,
) {

  /**
   * Downloads the survey with the specified ID and related LOIs from remote and inserts and/or
   * updates them on the local device.
   *
   * @return Updated [Survey] or `null` if the survey could not be found.
   * @throws error if the remote query fails.
   */
  suspend operator fun invoke(surveyId: String): Survey? =
    remoteSurveyRepository.fetchSurvey(surveyId)?.also {
      localSurveyRepository.saveSurvey(it)
      loiRepository.syncLocationsOfInterest(it)
    }
}
