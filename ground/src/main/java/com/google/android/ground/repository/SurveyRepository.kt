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
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.remote.NotFoundException
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.map.CameraPosition
import com.google.common.collect.ImmutableList
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import java.util.concurrent.TimeUnit
import java8.util.Optional
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitSingleOrNull
import kotlinx.coroutines.withContext
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
  private val localDataStore: LocalDataStore,
  private val remoteDataStore: RemoteDataStore,
  private val localValueStore: LocalValueStore,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  private val surveyStore = localDataStore.surveyStore

  /** Emits the latest loading state of the current survey on subscribe and on change. */
  val surveyLoadingState: @Hot(replays = true) FlowableProcessor<Loadable<Survey>> =
    BehaviorProcessor.create()

  /** Emits the last active survey or `empty()` if none available on subscribe and on change. */
  val activeSurvey: @Hot(replays = true) Flowable<Optional<Survey>>
    get() = surveyLoadingState.map { obj: Loadable<Survey> -> obj.value() }

  var activeSurveyId: String = ""
    private set

  val offlineSurveys: @Cold Single<ImmutableList<Survey>>
    get() = surveyStore.surveys

  private suspend fun syncSurveyFromRemote(surveyId: String): Survey {
    val survey = syncSurveyWithRemote(surveyId).await()
    remoteDataStore.subscribeToSurveyUpdates(surveyId).await()
    return survey
  }

  /** This only works if the survey is already cached to local db. */
  fun getSurvey(surveyId: String): @Cold Single<Survey> =
    surveyStore
      .getSurveyById(surveyId)
      .switchIfEmpty(Single.error { NotFoundException("Survey not found $surveyId") })

  fun syncSurveyWithRemote(id: String): @Cold Single<Survey> =
    remoteDataStore
      .loadSurvey(id)
      .timeout(LOAD_REMOTE_SURVEY_TIMEOUT_SECS, TimeUnit.SECONDS)
      .flatMap { surveyStore.insertOrUpdateSurvey(it).toSingleDefault(it) }
      .doOnSubscribe { Timber.d("Loading survey $id") }
      .doOnError { err -> Timber.d(err, "Error loading survey from remote") }

  fun loadLastActiveSurvey() = activateSurvey(localValueStore.lastActiveSurveyId)

  fun activateSurvey(surveyId: String) {
    // Do nothing if survey is already active.
    if (surveyId == activeSurveyId) {
      return
    }
    // Clear survey if id is empty.
    if (surveyId.isEmpty()) {
      clearActiveSurvey()
      return
    }

    externalScope.launch {
      withContext(ioDispatcher) {
        try {
          surveyLoadingState.onNext(Loadable.loading())
          val survey =
            surveyStore.getSurveyById(surveyId).awaitSingleOrNull()
              ?: syncSurveyFromRemote(surveyId)
          activeSurveyId = surveyId
          localValueStore.lastActiveSurveyId = surveyId
          surveyLoadingState.onNext(Loadable.loaded(survey))
        } catch (e: Error) {
          Timber.e("Error activating survey", e)
          surveyLoadingState.onNext(Loadable.error(e))
        }
      }
    }
  }

  fun clearActiveSurvey() {
    surveyLoadingState.onNext(Loadable.notLoaded())
  }

  fun getSurveySummaries(user: User): @Cold Flowable<Loadable<List<Survey>>> =
    loadSurveySummariesFromRemote(user)
      .doOnSubscribe { Timber.d("Loading survey list from remote") }
      .doOnError { Timber.d(it, "Failed to load survey list from remote") }
      .onErrorResumeNext { offlineSurveys }
      .toFlowable()
      .compose { Loadable.loadingOnceAndWrap(it) }

  private fun loadSurveySummariesFromRemote(user: User): @Cold Single<List<Survey>> =
    remoteDataStore
      .loadSurveySummaries(user)
      .timeout(LOAD_REMOTE_SURVEY_SUMMARIES_TIMEOUT_SECS, TimeUnit.SECONDS)

  fun getMutationsOnceAndStream(
    survey: Survey
  ): @Cold(terminates = false) Flowable<ImmutableList<Mutation>> {
    return localDataStore.getMutationsOnceAndStream(survey)
  }

  fun setCameraPosition(surveyId: String, cameraPosition: CameraPosition) =
    localValueStore.setLastCameraPosition(surveyId, cameraPosition)

  fun getLastCameraPosition(surveyId: String): CameraPosition? =
    localValueStore.getLastCameraPosition(surveyId)
}
