/*
 * Copyright 2018 Google LLC
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
package org.groundplatform.android.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.groundplatform.android.FirebaseCrashLogger
import org.groundplatform.android.coroutines.ApplicationScope
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.remote.RemoteDataStore
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.model.User
import timber.log.Timber

private const val ACTIVATE_SURVEY_TIMEOUT_MILLS: Long = 3 * 1000
private const val LOAD_REMOTE_SURVEY_TIMEOUT_MILLS: Long = 15 * 1000

/** Maintains the state of currently active survey. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SurveyRepository
@Inject
constructor(
  @ApplicationScope private val externalScope: CoroutineScope,
  private val firebaseCrashLogger: FirebaseCrashLogger,
  private val localSurveyStore: LocalSurveyStore,
  private val localValueStore: LocalValueStore,
  private val remoteDataStore: RemoteDataStore,
) {
  private val _selectedSurveyId = MutableStateFlow<String?>(null)

  val activeSurveyFlow: StateFlow<Survey?> =
    _selectedSurveyId
      .flatMapLatest { id -> getOfflineSurveyFlow(id) }
      .stateIn(externalScope, SharingStarted.Lazily, null)

  /** The currently active survey, or `null` if no survey is active. */
  val activeSurvey: Survey?
    get() = activeSurveyFlow.value

  suspend fun saveSurvey(survey: Survey) = localSurveyStore.insertOrUpdateSurvey(survey)

  suspend fun getRemoteSurvey(surveyId: String): Survey? =
    withTimeoutOrNull(LOAD_REMOTE_SURVEY_TIMEOUT_MILLS) { remoteDataStore.loadSurvey(surveyId) }

  fun getRemoteSurveys(user: User): Flow<List<SurveyListItem>> =
    combine(remoteDataStore.getRestrictedSurveyList(user), remoteDataStore.getPublicSurveyList()) {
      restrictedSurveys,
      publicSurveys ->
      restrictedSurveys + publicSurveys
    }

  suspend fun getOfflineSurvey(surveyId: String): Survey? = localSurveyStore.getSurveyById(surveyId)

  fun getOfflineSurveys(): Flow<List<Survey>> = localSurveyStore.surveys

  suspend fun removeOfflineSurvey(surveyId: String) {
    getOfflineSurvey(surveyId)?.let { localSurveyStore.deleteSurvey(it) }
  }

  private fun getOfflineSurveyFlow(id: String?): Flow<Survey?> =
    if (id.isNullOrBlank()) flowOf(null) else localSurveyStore.survey(id)

  /**
   * Activates the survey with the specified id. Waits for [ACTIVATE_SURVEY_TIMEOUT_MILLS] before
   * throwing an error if the survey couldn't be activated.
   */
  suspend fun activateSurvey(surveyId: String) {
    _selectedSurveyId.update { surveyId }

    // Wait for survey to be updated. Else throw an error after timeout.
    try {
      withTimeout(ACTIVATE_SURVEY_TIMEOUT_MILLS) {
        activeSurveyFlow.first { survey ->
          if (surveyId.isBlank()) {
            survey == null
          } else {
            survey?.id == surveyId
          }
        }
      }
    } catch (e: TimeoutCancellationException) {
      Timber.e(e, "Failed to get survey due to timeout")
    }

    if (isSurveyActive(surveyId) || surveyId.isBlank()) {
      firebaseCrashLogger.setSelectedSurveyId(surveyId)
      localValueStore.lastActiveSurveyId = surveyId
    }
  }

  suspend fun clearActiveSurvey() {
    activateSurvey("")
  }

  fun isSurveyActive(surveyId: String): Boolean =
    surveyId.isNotBlank() && activeSurvey?.id == surveyId
}
