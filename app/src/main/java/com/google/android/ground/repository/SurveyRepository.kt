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
package com.google.android.ground.repository

import com.google.android.ground.FirebaseCrashLogger
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.model.Survey
import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.model.User
import com.google.android.ground.model.toListItem
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.system.NetworkManager
import com.google.android.ground.system.NetworkStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val LOAD_REMOTE_SURVEY_TIMEOUT_MILLS: Long = 15 * 1000

/**
 * Coordinates persistence and retrieval of [Survey] instances from remote, local, and in memory
 * data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SurveyRepository
@Inject
constructor(
  private val firebaseCrashLogger: FirebaseCrashLogger,
  private val localSurveyStore: LocalSurveyStore,
  private val remoteDataStore: RemoteDataStore,
  private val localValueStore: LocalValueStore,
  private val networkManager: NetworkManager,
  @ApplicationScope private val externalScope: CoroutineScope,
) {
  private val _selectedSurveyIdFlow = MutableStateFlow<String?>(null)
  var selectedSurveyId: String?
    get() = _selectedSurveyIdFlow.value
    set(value) {
      _selectedSurveyIdFlow.update { value }
      firebaseCrashLogger.setSelectedSurveyId(value)
    }

  val activeSurveyFlow: StateFlow<Survey?> =
    _selectedSurveyIdFlow
      .flatMapLatest { id -> offlineSurvey(id) }
      .stateIn(externalScope, SharingStarted.Lazily, null)

  /** The currently active survey, or `null` if no survey is active. */
  val activeSurvey: Survey?
    get() = activeSurveyFlow.value

  val localSurveyListFlow: Flow<List<SurveyListItem>>
    get() = localSurveyStore.surveys.map { list -> list.map { it.toListItem(true) } }

  /** The id of the last activated survey. */
  var lastActiveSurveyId: String by localValueStore::lastActiveSurveyId
    internal set

  init {
    activeSurveyFlow.filterNotNull().onEach { lastActiveSurveyId = it.id }.launchIn(externalScope)
  }

  /** Listens for remote changes to the survey with the specified id. */
  suspend fun subscribeToSurveyUpdates(surveyId: String) =
    remoteDataStore.subscribeToSurveyUpdates(surveyId)

  /**
   * Returns the survey with the specified id from the local db, or `null` if not available offline.
   */
  suspend fun getOfflineSurvey(surveyId: String): Survey? = localSurveyStore.getSurveyById(surveyId)

  private fun offlineSurvey(id: String?): Flow<Survey?> =
    if (id == null) flowOf(null) else localSurveyStore.survey(id)

  /**
   * Loads the survey with the specified id from remote and writes to local db. If the survey isn't
   * found or operation times out, then we return null and don't fetch the survey from local db.
   *
   * @throws error if the remote query fails.
   */
  suspend fun loadAndSyncSurveyWithRemote(id: String): Survey? =
    withTimeoutOrNull(LOAD_REMOTE_SURVEY_TIMEOUT_MILLS) {
        Timber.d("Loading survey $id")
        remoteDataStore.loadSurvey(id)
      }
      ?.apply { localSurveyStore.insertOrUpdateSurvey(this) }

  fun clearActiveSurvey() {
    selectedSurveyId = null
  }

  fun getSurveyList(user: User): Flow<List<SurveyListItem>> =
    @OptIn(ExperimentalCoroutinesApi::class)
    networkManager.networkStatusFlow.flatMapLatest { networkStatus ->
      if (networkStatus == NetworkStatus.AVAILABLE) {
        getRemoteSurveyList(user)
      } else {
        localSurveyListFlow
      }
    }

  private fun getRemoteSurveyList(user: User): Flow<List<SurveyListItem>> =
    remoteDataStore.getSurveyList(user).combine(localSurveyListFlow) { remoteSurveys, localSurveys
      ->
      remoteSurveys.map { remoteSurvey ->
        remoteSurvey.copy(availableOffline = localSurveys.any { it.id == remoteSurvey.id })
      }
    }

  /** Attempts to remove the locally synced survey. Doesn't throw an error if it doesn't exist. */
  suspend fun removeOfflineSurvey(surveyId: String) {
    val survey = localSurveyStore.getSurveyById(surveyId)
    survey?.let { localSurveyStore.deleteSurvey(survey) }
    if (activeSurvey?.id == surveyId) {
      clearActiveSurvey()
    }
  }
}
