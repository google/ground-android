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
import com.google.android.ground.model.User
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.dao.TileSourceDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import java8.util.Optional
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxCompletable
import timber.log.Timber

private const val LOAD_REMOTE_SURVEY_TIMEOUT_SECS: Long = 15
private const val LOAD_REMOTE_SURVEY_SUMMARIES_TIMEOUT_SECS: Long = 30

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
  private val tileSourceDao: TileSourceDao,
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
  val activeSurveyFlowable: @Cold Flowable<Optional<Survey>> =
    activeSurveyFlow.map { if (it == null) Optional.empty() else Optional.of(it) }.asFlowable()

  val offlineSurveys: @Cold Flowable<List<Survey>>
    get() = localSurveyStore.surveys.asFlowable()

  var lastActiveSurveyId: String by localValueStore::lastActiveSurveyId
    internal set

  /** Listens for remote changes to the survey with the specified id. */
  suspend fun subscribeToSurveyUpdates(surveyId: String) =
    remoteDataStore.subscribeToSurveyUpdates(surveyId).await()

  /**
   * Returns the survey with the specified id from the local db, or `null` if not available offline.
   */
  suspend fun getOfflineSurveySuspend(surveyId: String): Survey? =
    localSurveyStore.getSurveyByIdSuspend(surveyId)

  fun syncSurveyWithRemote(id: String): @Cold Single<Survey> =
    remoteDataStore
      .loadSurvey(id)
      .timeout(LOAD_REMOTE_SURVEY_TIMEOUT_SECS, TimeUnit.SECONDS)
      .flatMap { rxCompletable { localSurveyStore.insertOrUpdateSurvey(it) }.toSingleDefault(it) }
      .doOnSuccess {
        // TODO: Define and use a BaseMapStore
        it.tileSources.forEach { bm ->
          tileSourceDao.insertOrUpdate(bm.toLocalDataStoreObject(it.id))
        }
      }
      .doOnSubscribe { Timber.d("Loading survey $id") }
      .doOnError { err -> Timber.d(err, "Error loading survey from remote") }

  fun clearActiveSurvey() {
    activeSurvey = null
  }

  fun getSurveySummaries(user: User): @Cold Single<List<Survey>> =
    loadSurveySummariesFromRemote(user)
      .doOnSubscribe { Timber.d("Loading survey list from remote") }
      .doOnError { Timber.d(it, "Failed to load survey list from remote") }
      .onErrorResumeNext { offlineSurveys.single(listOf()) }

  private fun loadSurveySummariesFromRemote(user: User): @Cold Single<List<Survey>> =
    remoteDataStore
      .loadSurveySummaries(user)
      .timeout(LOAD_REMOTE_SURVEY_SUMMARIES_TIMEOUT_SECS, TimeUnit.SECONDS)

  /** Attempts to remove the locally synced survey. Doesn't throw an error if it doesn't exist. */
  suspend fun removeOfflineSurvey(surveyId: String) {
    val survey = localSurveyStore.getSurveyByIdSuspend(surveyId)
    survey?.let { localSurveyStore.deleteSurvey(survey) }
    if (activeSurvey?.id == surveyId) {
      clearActiveSurvey()
    }
  }
}
