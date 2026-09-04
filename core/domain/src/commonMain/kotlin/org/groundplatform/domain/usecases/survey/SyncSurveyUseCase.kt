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

import co.touchlab.kermit.Logger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveySyncMode
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.SurveyRepositoryInterface

/**
 * Loads the survey with the specified id and related LOIs from remote and writes to local db.
 *
 * If the survey isn't found or operation times out, then we return null. Otherwise returns the
 * updated [Survey].
 *
 * @throws error if the remote query fails.
 */
class SyncSurveyUseCase(
  private val loiRepository: LocationOfInterestRepositoryInterface,
  private val surveyRepository: SurveyRepositoryInterface,
) {

  suspend operator fun invoke(surveyId: String): Survey? =
    fetchSurvey(surveyId)?.also { syncSurvey(it) }

  private suspend fun fetchSurvey(surveyId: String): Survey? {
    Logger.d("Loading survey $surveyId")
    return surveyRepository.getRemoteSurvey(surveyId)
  }

  private suspend fun syncSurvey(survey: Survey) {
    surveyRepository.saveSurvey(survey)
    val result = loiRepository.syncLocationsOfInterest(survey, syncMode(survey))
    surveyRepository.recordSyncState(survey, result)
    Logger.d("Synced survey ${survey.id}")
  }

  private suspend fun syncMode(survey: Survey): SurveySyncMode {
    val syncState = surveyRepository.getSyncState(survey.id)
    return when {
      syncState == null -> SurveySyncMode.Full
      survey.dataVisibility != syncState.syncedDataVisibility -> SurveySyncMode.Full
      Clock.System.now().toEpochMilliseconds() - syncState.lastFullSyncClientTimestamp >
        FULL_SYNC_INTERVAL_MILLIS -> SurveySyncMode.Full
      else -> SurveySyncMode.Incremental(syncState.latestLoiServerTimestamp)
    }
  }

  private companion object {
    // An undelivered FCM is stored for a max of 28 days
    val FULL_SYNC_INTERVAL_MILLIS = 28.days.inWholeMilliseconds
  }
}
