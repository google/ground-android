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
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.repository.LocationOfInterestRepository
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val LOAD_REMOTE_SURVEY_TIMEOUT_MILLS: Long = 15 * 1000

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
  private val localSurveyStore: LocalSurveyStore,
  private val loiRepository: LocationOfInterestRepository,
  private val remoteDataStore: RemoteDataStore,
) {

  suspend operator fun invoke(surveyId: String): Survey? =
    fetchSurvey(surveyId)?.also { syncSurvey(it) }

  private suspend fun fetchSurvey(surveyId: String): Survey? =
    withTimeoutOrNull(LOAD_REMOTE_SURVEY_TIMEOUT_MILLS) {
      Timber.d("Loading survey $surveyId")
      remoteDataStore.loadSurvey(surveyId)
    }

  private suspend fun syncSurvey(survey: Survey) {
    localSurveyStore.insertOrUpdateSurvey(survey)
    loiRepository.syncLocationsOfInterest(survey)
    Timber.d("Synced survey ${survey.id}")
  }
}
