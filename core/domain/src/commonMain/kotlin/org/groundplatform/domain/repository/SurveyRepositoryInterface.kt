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
import org.groundplatform.domain.model.User

/** Maintains the state of currently active survey. */
interface SurveyRepositoryInterface {
  val activeSurveyFlow: StateFlow<Survey?>
  /** The currently active survey, or `null` if no survey is active. */
  val activeSurvey: Survey?

  suspend fun saveSurvey(survey: Survey)

  suspend fun getRemoteSurvey(surveyId: String): Survey?

  fun getRemoteSurveys(user: User): Flow<List<SurveyListItem>>

  suspend fun getOfflineSurvey(surveyId: String): Survey?

  fun getOfflineSurveys(): Flow<List<Survey>>

  suspend fun removeOfflineSurvey(surveyId: String)

  suspend fun activateSurvey(surveyId: String)

  suspend fun clearActiveSurvey()

  fun isSurveyActive(surveyId: String): Boolean
}
