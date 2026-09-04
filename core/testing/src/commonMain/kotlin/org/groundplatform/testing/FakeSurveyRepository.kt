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
package org.groundplatform.testing

import kotlin.collections.plus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.model.SurveySyncState
import org.groundplatform.domain.model.User
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface.SyncResult
import org.groundplatform.domain.repository.SurveyRepositoryInterface

class FakeSurveyRepository : SurveyRepositoryInterface {
  private val _activeSurveyFlow = MutableStateFlow<Survey?>(null)
  override val activeSurveyFlow: StateFlow<Survey?> = _activeSurveyFlow
  override val activeSurvey: Survey?
    get() = _activeSurveyFlow.value

  override var lastActiveSurveyId: String = ""

  val offlineSurveysFlow = MutableStateFlow<List<Survey>>(emptyList())
  var offlineSurveys: List<Survey>
    get() = offlineSurveysFlow.value
    set(value) {
      offlineSurveysFlow.value = value
    }

  var remoteSurveys: List<Survey> = emptyList()

  var syncState: SurveySyncState? = null

  var lastRecordedSyncState: SyncResult? = null

  val remoteListItemsFlow = MutableStateFlow<List<SurveyListItem>>(emptyList())
  var remoteListItems: List<SurveyListItem>
    get() = remoteListItemsFlow.value
    set(value) {
      remoteListItemsFlow.value = value
    }

  /** Ids of surveys currently subscribed to via [subscribeToSurveyUpdates]. */
  val subscribedSurveyIds = mutableSetOf<String>()

  val onGetRemoteSurveyCall = FakeCall<String, Survey?> { id -> remoteSurveys.find { it.id == id } }

  private val dataSharingConsentMap = mutableMapOf<String, Boolean>()

  override suspend fun saveSurvey(survey: Survey) {
    offlineSurveys = offlineSurveys.filterNot { it.id == survey.id } + survey
  }

  override suspend fun getSyncState(surveyId: String): SurveySyncState? = syncState

  override suspend fun recordSyncState(survey: Survey, loiSyncResult: SyncResult) {
    lastRecordedSyncState = loiSyncResult
  }

  override suspend fun getRemoteSurvey(surveyId: String): Survey? = onGetRemoteSurveyCall(surveyId)

  override fun getRemoteSurveys(user: User): Flow<List<SurveyListItem>> = remoteListItemsFlow

  override suspend fun getOfflineSurvey(surveyId: String): Survey? = offlineSurveys.find {
    it.id == surveyId
  }

  override fun getOfflineSurveys(): Flow<List<Survey>> = offlineSurveysFlow

  override suspend fun removeOfflineSurvey(surveyId: String) {
    offlineSurveys = offlineSurveys.filterNot { it.id == surveyId }
  }

  override suspend fun activateSurvey(surveyId: String) {
    _activeSurveyFlow.value = offlineSurveys.find { it.id == surveyId }
    lastActiveSurveyId = surveyId
  }

  override suspend fun clearActiveSurvey() {
    _activeSurveyFlow.value = null
    lastActiveSurveyId = ""
  }

  override fun isSurveyActive(surveyId: String): Boolean = _activeSurveyFlow.value?.id == surveyId

  override suspend fun subscribeToSurveyUpdates(surveyId: String) {
    subscribedSurveyIds.add(surveyId)
  }

  override suspend fun unsubscribeFromSurveyUpdates(surveyId: String) {
    subscribedSurveyIds.remove(surveyId)
  }

  override fun getDataSharingConsent(surveyId: String): Boolean =
    dataSharingConsentMap[surveyId] ?: false

  override fun setDataSharingConsent(surveyId: String, consent: Boolean) {
    dataSharingConsentMap[surveyId] = consent
  }
}
