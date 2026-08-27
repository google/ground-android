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
package org.groundplatform.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.groundplatform.data.stores.LocalSurveyStore
import org.groundplatform.domain.model.Survey

class FakeLocalSurveyStore : LocalSurveyStore {
  private val surveyMap = MutableStateFlow<Map<String, Survey>>(emptyMap())

  override val surveys: Flow<List<Survey>> = surveyMap.map { it.values.toList() }

  override fun survey(id: String): Flow<Survey?> = surveyMap.map { it[id] }

  override suspend fun getSurveyById(id: String): Survey? = surveyMap.value[id]

  override suspend fun deleteSurvey(survey: Survey) {
    surveyMap.value = surveyMap.value - survey.id
  }

  override suspend fun insertOrUpdateSurvey(survey: Survey) {
    surveyMap.value = surveyMap.value + (survey.id to survey)
  }
}
