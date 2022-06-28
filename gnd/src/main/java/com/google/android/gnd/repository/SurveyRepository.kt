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
package com.google.android.gnd.repository

import com.google.android.gnd.model.Role
import com.google.android.gnd.model.Survey
import com.google.android.gnd.model.User
import com.google.android.gnd.model.feature.FeatureType
import com.google.android.gnd.model.job.Job
import com.google.android.gnd.model.mutation.Mutation
import com.google.android.gnd.persistence.local.LocalDataStore
import com.google.android.gnd.persistence.local.LocalValueStore
import com.google.android.gnd.persistence.remote.NotFoundException
import com.google.android.gnd.persistence.remote.RemoteDataStore
import com.google.android.gnd.rx.Loadable
import com.google.android.gnd.rx.annotations.Cold
import com.google.android.gnd.rx.annotations.Hot
import com.google.android.gnd.ui.map.CameraPosition
import com.google.android.gnd.util.toImmutableList
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import java8.util.Optional
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val LOAD_REMOTE_SURVEY_TIMEOUT_SECS: Long = 15
private const val LOAD_REMOTE_SURVEY_SUMMARIES_TIMEOUT_SECS: Long = 30

/**
 * Coordinates persistence and retrieval of [Survey] instances from remote, local, and in
 * memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
class SurveyRepository @Inject constructor(
    private val userRepository: UserRepository,
    private val localDataStore: LocalDataStore,
    private val remoteDataStore: RemoteDataStore,
    private val localValueStore: LocalValueStore
) {

    /** Emits a survey id on {@see #activateSurvey} and empty on {@see #clearActiveSurvey}.  */
    private val selectSurveyEvent: @Hot FlowableProcessor<String> = PublishProcessor.create()

    /** Emits the latest loading state of the current survey on subscribe and on change.  */
    val surveyLoadingState: @Hot(replays = true) FlowableProcessor<Loadable<Survey>> =
        BehaviorProcessor.create()

    var lastActiveSurveyId: String
        get() = localValueStore.lastActiveSurveyId
        set(value) {
            localValueStore.lastActiveSurveyId = value
        }

    val activeSurvey: @Hot(replays = true) Flowable<Optional<Survey>>
        get() = surveyLoadingState.map { obj: Loadable<Survey> -> obj.value() }

    val offlineSurveys: @Cold Single<ImmutableList<Survey>>
        get() = localDataStore.surveys

    init {
        // Kicks off the loading process whenever a new survey id is selected.
        selectSurveyEvent
            .distinctUntilChanged()
            .switchMap { selectSurvey(it) }
            .onBackpressureLatest()
            .subscribe(surveyLoadingState)
    }

    private fun selectSurvey(surveyId: String): @Cold Flowable<Loadable<Survey>> {
        // Empty id indicates intent to deactivate the current survey. Used on sign out.
        return if (surveyId.isEmpty())
            Flowable.just(Loadable.notLoaded())
        else
            syncSurveyWithRemote(surveyId)
                .onErrorResumeNext { getSurvey(surveyId) }
                .map { attachJobPermissions(it) }
                .doOnSuccess { lastActiveSurveyId = surveyId }
                .toFlowable()
                .compose { Loadable.loadingOnceAndWrap(it) }
    }

    private fun attachJobPermissions(survey: Survey): Survey {
        val userRole = userRepository.getUserRole(survey)
        // TODO: Use Map once migration of dependencies to Kotlin is complete.
        val jobs: ImmutableMap.Builder<String, Job> = ImmutableMap.builder()
        for (job in survey.jobs) {
            jobs.put(
                job.id,
                job.toBuilder().setUserCanAdd(getAddableFeatureTypes(userRole)).build()
            )
        }
        return survey.toBuilder().setJobMap(jobs.build()).build()
    }

    private fun getAddableFeatureTypes(userRole: Role): ImmutableList<FeatureType> =
        when (userRole) {
            Role.OWNER, Role.SURVEY_ORGANIZER -> FeatureType.ALL
            else -> ImmutableList.of()
        }

    /** This only works if the survey is already cached to local db.  */
    fun getSurvey(surveyId: String): @Cold Single<Survey> =
        localDataStore
            .getSurveyById(surveyId)
            .switchIfEmpty(Single.error { NotFoundException("Survey not found $surveyId") })

    private fun syncSurveyWithRemote(id: String): @Cold Single<Survey> =
        remoteDataStore
            .loadSurvey(id)
            .timeout(LOAD_REMOTE_SURVEY_TIMEOUT_SECS, TimeUnit.SECONDS)
            .flatMap { localDataStore.insertOrUpdateSurvey(it).toSingleDefault(it) }
            .doOnSubscribe { Timber.d("Loading survey $id") }
            .doOnError { err -> Timber.d(err, "Error loading survey from remote") }

    fun loadLastActiveSurvey() = activateSurvey(lastActiveSurveyId)

    fun activateSurvey(surveyId: String) = selectSurveyEvent.onNext(surveyId)

    fun clearActiveSurvey() = selectSurveyEvent.onNext("")

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

    fun getModifiableJobs(survey: Survey): ImmutableList<Job> =
        survey.jobs
            .filter { !it.userCanAdd.isEmpty() }
            .toImmutableList()

    fun getMutationsOnceAndStream(survey: Survey): @Cold(terminates = false) Flowable<ImmutableList<Mutation>> {
        return localDataStore.getMutationsOnceAndStream(survey)
    }

    fun setCameraPosition(surveyId: String, cameraPosition: CameraPosition) =
        localValueStore.setLastCameraPosition(surveyId, cameraPosition)

    fun getLastCameraPosition(surveyId: String): Optional<CameraPosition> =
        localValueStore.getLastCameraPosition(surveyId)
}