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
package org.groundplatform.android.persistence.local.stores

import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.model.Survey

/** Provides access to [Survey] data in local storage. */
interface LocalSurveyStore {
  /** Load surveys stored in local database. */
  val surveys: Flow<List<Survey>>

  fun survey(id: String): Flow<Survey?>

  /** Load last active survey, if any. */
  suspend fun getSurveyById(id: String): Survey?

  /** Delete stored survey from database. */
  suspend fun deleteSurvey(survey: Survey)

  /** Add survey to the database. */
  suspend fun insertOrUpdateSurvey(survey: Survey)
}
