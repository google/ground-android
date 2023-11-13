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

import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.model.Survey
import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.model.User
import com.google.android.ground.model.toListItem
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.NetworkManager
import com.google.android.ground.system.NetworkStatus
import io.reactivex.Flowable
import java8.util.Optional
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val LOAD_REMOTE_SURVEY_TIMEOUT_MILLS: Long = 15 * 1000

/**
 * Coordinates persistence and retrieval of [Survey] instances from remote, local, and in memory
 * data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
class SurveyRepository
@Inject
constructor(
  private val localSurveyStore: LocalSurveyStore,
  private val remoteDataStore: RemoteDataStore,
  private val localValueStore: LocalValueStore,
  private val networkManager: NetworkManager,
  @ApplicationScope private val externalScope: CoroutineScope
) {
  private val _activeSurvey = MutableStateFlow<Survey?>(null)

  val activeSurveyFlow: SharedFlow<Survey?> =
    _activeSurvey.shareIn(externalScope, replay = 1, started = SharingStarted.Eagerly)

  /**
   * The currently active survey, or `null` if no survey is active. Updating this property causes
   * [lastActiveSurveyId] to be updated with the id of the specified survey, or `""` if the
   * specified survey is `null`.
   */
  var activeSurvey: Survey?
    get() = _activeSurvey.value
    set(value) {
      _activeSurvey.value = value
      lastActiveSurveyId = value?.id ?: ""
    }

  /**
   * Emits the currently active survey on subscribe and on change. Emits `empty()` when no survey is
   * active or local db isn't up-to-date.
   */
  // TODO(#1593): Update callers to use [activeSurveyFlow] and delete this member.
  @Deprecated("Use activeSurveyFlow instead")
  val activeSurveyFlowable: @Cold Flowable<Optional<Survey>> =
    activeSurveyFlow.map { if (it == null) Optional.empty() else Optional.of(it) }.asFlowable()

  val localSurveyListFlow: Flow<List<SurveyListItem>>
    get() = localSurveyStore.surveys.map { list -> list.map { it.toListItem(true) } }

  var lastActiveSurveyId: String by localValueStore::lastActiveSurveyId
    internal set

  /** Listens for remote changes to the survey with the specified id. */
  suspend fun subscribeToSurveyUpdates(surveyId: String) =
    remoteDataStore.subscribeToSurveyUpdates(surveyId)

  /**
   * Returns the survey with the specified id from the local db, or `null` if not available offline.
   */
  suspend fun getOfflineSurvey(surveyId: String): Survey? = localSurveyStore.getSurveyById(surveyId)

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
    activeSurvey = null
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
