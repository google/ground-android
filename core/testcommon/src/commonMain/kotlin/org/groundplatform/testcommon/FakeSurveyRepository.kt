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
package org.groundplatform.testcommon

import kotlin.collections.plus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.model.User
import org.groundplatform.domain.repository.SurveyRepositoryInterface

class FakeSurveyRepository : SurveyRepositoryInterface {
  private val _activeSurveyFlow = MutableStateFlow<Survey?>(null)
  override val activeSurveyFlow: StateFlow<Survey?> = _activeSurveyFlow
  override val activeSurvey: Survey?
    get() = _activeSurveyFlow.value

  var offlineSurveys: List<Survey> = emptyList()
  var remoteSurveys: List<Survey> = emptyList()
  var remoteListItems: List<SurveyListItem> = emptyList()

  val onGetRemoteSurveyCall = FakeCall<String, Survey?> { id -> remoteSurveys.find { it.id == id } }

  override suspend fun saveSurvey(survey: Survey) {
    offlineSurveys + survey
  }

  override suspend fun getRemoteSurvey(surveyId: String): Survey? = onGetRemoteSurveyCall(surveyId)

  override fun getRemoteSurveys(user: User): Flow<List<SurveyListItem>> = flowOf(remoteListItems)

  override suspend fun getOfflineSurvey(surveyId: String): Survey? = offlineSurveys.find {
    it.id == surveyId
  }

  override fun getOfflineSurveys(): Flow<List<Survey>> = flowOf(offlineSurveys)

  override suspend fun removeOfflineSurvey(surveyId: String) {
    offlineSurveys = offlineSurveys.filterNot { it.id == surveyId }
  }

  override suspend fun activateSurvey(surveyId: String) {
    _activeSurveyFlow.value = offlineSurveys.find { it.id == surveyId }
  }

  override suspend fun clearActiveSurvey() {
    _activeSurveyFlow.value = null
  }

  override fun isSurveyActive(surveyId: String): Boolean = _activeSurveyFlow.value?.id == surveyId
}
