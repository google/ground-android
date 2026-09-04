/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.model.SurveySyncState
import org.groundplatform.domain.model.User

/** Maintains the state of currently active survey. */
interface SurveyRepositoryInterface {
  val activeSurveyFlow: StateFlow<Survey?>
  /** The currently active survey, or `null` if no survey is active. */
  val activeSurvey: Survey?

  /** The ID of the last survey successfully activated by the user, or empty string if none. */
  val lastActiveSurveyId: String

  suspend fun saveSurvey(survey: Survey)

  /** Returns what the last sync of the given survey left behind, or null if none has run. */
  suspend fun getSyncState(surveyId: String): SurveySyncState?

  /** Records where [loiSyncResult] left the sync of [survey], for the next one to resume from. */
  suspend fun recordSyncState(
    survey: Survey,
    loiSyncResult: LocationOfInterestRepositoryInterface.SyncResult,
  )

  suspend fun getRemoteSurvey(surveyId: String): Survey?

  fun getRemoteSurveys(user: User): Flow<List<SurveyListItem>>

  suspend fun getOfflineSurvey(surveyId: String): Survey?

  fun getOfflineSurveys(): Flow<List<Survey>>

  suspend fun removeOfflineSurvey(surveyId: String)

  suspend fun activateSurvey(surveyId: String)

  suspend fun clearActiveSurvey()

  /** Returns true if the survey with [surveyId] is currently active. */
  fun isSurveyActive(surveyId: String): Boolean

  suspend fun subscribeToSurveyUpdates(surveyId: String)

  suspend fun unsubscribeFromSurveyUpdates(surveyId: String)

  /** Returns true if the user has agreed to data sharing terms for the given [surveyId]. */
  fun getDataSharingConsent(surveyId: String): Boolean

  /** Records the user's [consent] to data sharing terms for the given [surveyId]. */
  fun setDataSharingConsent(surveyId: String, consent: Boolean)
}
