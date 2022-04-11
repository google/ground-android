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

import com.google.android.gnd.model.Mutation
import com.google.android.gnd.model.Project
import com.google.android.gnd.model.Role
import com.google.android.gnd.model.User
import com.google.android.gnd.model.feature.FeatureType
import com.google.android.gnd.model.layer.Layer
import com.google.android.gnd.persistence.local.LocalDataStore
import com.google.android.gnd.persistence.local.LocalValueStore
import com.google.android.gnd.persistence.remote.NotFoundException
import com.google.android.gnd.persistence.remote.RemoteDataStore
import com.google.android.gnd.rx.Loadable
import com.google.android.gnd.rx.annotations.Cold
import com.google.android.gnd.rx.annotations.Hot
import com.google.android.gnd.ui.map.CameraPosition
import com.google.android.gnd.util.ImmutableListCollector
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import java8.util.Optional
import java8.util.stream.StreamSupport
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates persistence and retrieval of [Project] instances from remote, local, and in
 * memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
class ProjectRepository @Inject constructor(
    private val userRepository: UserRepository,
    private val localDataStore: LocalDataStore,
    private val remoteDataStore: RemoteDataStore,
    private val localValueStore: LocalValueStore) {

    /** Emits a project id on {@see #activateProject} and empty on {@see #clearActiveProject}.  */
    private val selectProjectEvent: @Hot FlowableProcessor<Optional<String>> = PublishProcessor.create()

    /** Emits the latest loading state of the current project on subscribe and on change.  */
    private val projectLoadingState: @Hot(replays = true) FlowableProcessor<Loadable<Project>> = BehaviorProcessor.create()
    private fun activateProject(projectId: Optional<String>): @Cold Flowable<Loadable<Project>>? {
        // Empty id indicates intent to deactivate the current project. Used on sign out.
        if (projectId.isEmpty) {
            return Flowable.just(Loadable.notLoaded())
        }
        val id = projectId.get()
        return syncProjectWithRemote(id)
            .onErrorResumeNext(Function<Throwable, SingleSource<out Project>?> { __: Throwable? -> getProject(id) })
            .map { project: Project -> attachLayerPermissions(project) }
            .doOnSuccess { __: Project? -> localValueStore.setLastActiveProjectId(id) }
            .toFlowable()
            .compose { source: Flowable<Project>? -> Loadable.loadingOnceAndWrap(source) }
    }

    private fun attachLayerPermissions(project: Project): Project {
        val userRole = userRepository.getUserRole(project)
        val layers: ImmutableMap.Builder<*, *> = ImmutableMap.builder<Any, Any>()
        for (layer in project.layers) {
            layers.put(
                layer.id,
                layer.toBuilder().setUserCanAdd(getAddableFeatureTypes(userRole, layer)).build())
        }
        return project.toBuilder().setLayerMap(layers.build()).build()
    }

    private fun getAddableFeatureTypes(userRole: Role, layer: Layer): ImmutableList<FeatureType> {
        return when (userRole) {
            Role.OWNER, Role.MANAGER -> FeatureType.ALL
            Role.CONTRIBUTOR -> layer.contributorsCanAdd
            Role.UNKNOWN -> ImmutableList.of()
            else -> ImmutableList.of()
        }
    }

    /** This only works if the project is already cached to local db.  */
    fun getProject(projectId: String): @Cold Single<Project> {
        return localDataStore
            .getProjectById(projectId)
            .switchIfEmpty(Single.error { NotFoundException("Project not found $projectId") })
    }

    private fun syncProjectWithRemote(id: String): @Cold Single<Project> {
        return remoteDataStore
            .loadProject(id)
            .timeout(LOAD_REMOTE_PROJECT_TIMEOUT_SECS, TimeUnit.SECONDS)
            .flatMap { p: Project -> localDataStore.insertOrUpdateProject(p).toSingleDefault(p) }
            .doOnSubscribe { __: Disposable? -> Timber.d("Loading project %s", id) }
            .doOnError { err: Throwable? -> Timber.d(err, "Error loading project from remote") }
    }

    val lastActiveProjectId: Optional<String?>
        get() = Optional.ofNullable(localValueStore.lastActiveProjectId)

    /**
     * Returns an observable that emits the latest project activation state, and continues to emit
     * changes to that state until all subscriptions are disposed.
     */
    fun getProjectLoadingState(): @Hot(replays = true) Flowable<Loadable<Project>> {
        return projectLoadingState
    }

    val activeProject: @Hot(replays = true) Flowable<Optional<Project>>
        get() = projectLoadingState.map { obj: Loadable<Project> -> obj.value() }

    fun activateProject(projectId: String) {
        Timber.v("activateProject() called with %s", projectId)
        selectProjectEvent.onNext(Optional.of(projectId))
    }

    fun getProjectSummaries(user: User): @Cold Flowable<Loadable<List<Project>>>? {
        return loadProjectSummariesFromRemote(user)
            .doOnSubscribe(Consumer { __: Disposable? -> Timber.d("Loading project list from remote") })
            .doOnError { err: Throwable? -> Timber.d(err, "Failed to load project list from remote") }
            .onErrorResumeNext { __: Throwable? -> localDataStore.projects }
            .toFlowable()
            .compose { source: Flowable<List<Project>>? -> Loadable.loadingOnceAndWrap(source) }
    }

    val offlineProjects: @Cold Single<ImmutableList<Project>>?
        get() = localDataStore.projects

    private fun loadProjectSummariesFromRemote(user: User): @Cold Single<List<Project>>? {
        return remoteDataStore
            .loadProjectSummaries(user)
            .timeout(LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS, TimeUnit.SECONDS)
    }

    /** Clears the currently active project from cache.  */
    fun clearActiveProject() {
        selectProjectEvent.onNext(Optional.empty())
    }

    fun getModifiableLayers(project: Project): ImmutableList<Layer> {
        return StreamSupport.stream(project.layers)
            .filter { layer: Layer -> !layer.userCanAdd.isEmpty() }
            .collect<ImmutableList<Layer>, Any>(ImmutableListCollector.toImmutableList())
    }

    fun getMutationsOnceAndStream(project: Project?): Flowable<ImmutableList<Mutation<*>>> {
        return localDataStore.getMutationsOnceAndStream(project)
    }

    fun setCameraPosition(projectId: String?, cameraPosition: CameraPosition?) {
        localValueStore.setLastCameraPosition(projectId, cameraPosition)
    }

    fun getLastCameraPosition(projectId: String?): Optional<CameraPosition> {
        return localValueStore.getLastCameraPosition(projectId)
    }

    companion object {
        private const val LOAD_REMOTE_PROJECT_TIMEOUT_SECS: Long = 15
        private const val LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS: Long = 30
    }

    init {

        // Kicks off the loading process whenever a new project id is selected.
        selectProjectEvent
            .distinctUntilChanged()
            .switchMap { projectId: Optional<String> -> this.activateProject(projectId) }
            .onBackpressureLatest()
            .subscribe(projectLoadingState)
    }
}