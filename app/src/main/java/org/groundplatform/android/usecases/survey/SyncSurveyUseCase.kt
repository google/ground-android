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
import org.groundplatform.android.model.Survey
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.SurveyRepository
import timber.log.Timber

/**
 * Loads the survey with the specified id and related LOIs from remote and writes to local db.
 *
 * If the survey isn't found or operation times out, then we return null. Otherwise returns the
 * updated [Survey].
 *
 * @throws error if the remote query fails.
 */
class SyncSurveyUseCase
@Inject
constructor(
  private val loiRepository: LocationOfInterestRepository,
  private val surveyRepository: SurveyRepository,
) {

  suspend operator fun invoke(surveyId: String): Survey? =
    fetchSurvey(surveyId)?.also { syncSurvey(it) }

  private suspend fun fetchSurvey(surveyId: String): Survey? {
    Timber.d("Loading survey $surveyId")
    return surveyRepository.getRemoteSurvey(surveyId)
  }

  private suspend fun syncSurvey(survey: Survey) {
    surveyRepository.saveSurvey(survey)
    loiRepository.syncLocationsOfInterest(survey)
    Timber.d("Synced survey ${survey.id}")
  }
}
