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

import com.google.android.gnd.model.Project
import com.google.android.gnd.model.Role
import com.google.android.gnd.model.User
import com.google.android.gnd.model.feature.FeatureType
import com.google.android.gnd.model.layer.Layer
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

private const val LOAD_REMOTE_PROJECT_TIMEOUT_SECS: Long = 15
private const val LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS: Long = 30

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
    private val localValueStore: LocalValueStore
) {

    /** Emits a project id on {@see #activateProject} and empty on {@see #clearActiveProject}.  */
    private val selectProjectEvent: @Hot FlowableProcessor<String> = PublishProcessor.create()

    /** Emits the latest loading state of the current project on subscribe and on change.  */
    val projectLoadingState: @Hot(replays = true) FlowableProcessor<Loadable<Project>> =
        BehaviorProcessor.create()

    var lastActiveProjectId: String
        get() = localValueStore.lastActiveProjectId
        set(value) {
            localValueStore.lastActiveProjectId = value
        }

    val activeProject: @Hot(replays = true) Flowable<Optional<Project>>
        get() = projectLoadingState.map { obj: Loadable<Project> -> obj.value() }

    val offlineProjects: @Cold Single<ImmutableList<Project>>
        get() = localDataStore.projects

    init {
        // Kicks off the loading process whenever a new project id is selected.
        selectProjectEvent
            .distinctUntilChanged()
            .switchMap { selectProject(it) }
            .onBackpressureLatest()
            .subscribe(projectLoadingState)
    }

    private fun selectProject(projectId: String): @Cold Flowable<Loadable<Project>> {
        // Empty id indicates intent to deactivate the current project. Used on sign out.
        return if (projectId.isEmpty())
            Flowable.just(Loadable.notLoaded())
        else
            syncProjectWithRemote(projectId)
                .onErrorResumeNext { getProject(projectId) }
                .map { attachLayerPermissions(it) }
                .doOnSuccess { lastActiveProjectId = projectId }
                .toFlowable()
                .compose { Loadable.loadingOnceAndWrap(it) }
    }

    private fun attachLayerPermissions(project: Project): Project {
        val userRole = userRepository.getUserRole(project)
        // TODO: Use Map once migration of dependencies to Kotlin is complete.
        val layers: ImmutableMap.Builder<String, Layer> = ImmutableMap.builder()
        for (layer in project.layers) {
            layers.put(
                layer.id,
                layer.toBuilder().setUserCanAdd(getAddableFeatureTypes(userRole, layer)).build()
            )
        }
        return project.toBuilder().setLayerMap(layers.build()).build()
    }

    private fun getAddableFeatureTypes(userRole: Role, layer: Layer): ImmutableList<FeatureType> =
        when (userRole) {
            Role.OWNER, Role.MANAGER -> FeatureType.ALL
            Role.CONTRIBUTOR -> layer.contributorsCanAdd
            Role.UNKNOWN -> ImmutableList.of()
        }

    /** This only works if the project is already cached to local db.  */
    fun getProject(projectId: String): @Cold Single<Project> =
        localDataStore
            .getProjectById(projectId)
            .switchIfEmpty(Single.error { NotFoundException("Project not found $projectId") })

    private fun syncProjectWithRemote(id: String): @Cold Single<Project> =
        remoteDataStore
            .loadProject(id)
            .timeout(LOAD_REMOTE_PROJECT_TIMEOUT_SECS, TimeUnit.SECONDS)
            .flatMap { localDataStore.insertOrUpdateProject(it).toSingleDefault(it) }
            .doOnSubscribe { Timber.d("Loading project $id") }
            .doOnError { err -> Timber.d(err, "Error loading project from remote") }

    fun loadLastActiveProject() = activateProject(lastActiveProjectId)

    fun activateProject(projectId: String) = selectProjectEvent.onNext(projectId)

    fun clearActiveProject() = selectProjectEvent.onNext("")

    fun getProjectSummaries(user: User): @Cold Flowable<Loadable<List<Project>>> =
        loadProjectSummariesFromRemote(user)
            .doOnSubscribe { Timber.d("Loading project list from remote") }
            .doOnError { Timber.d(it, "Failed to load project list from remote") }
            .onErrorResumeNext { offlineProjects }
            .toFlowable()
            .compose { Loadable.loadingOnceAndWrap(it) }

    private fun loadProjectSummariesFromRemote(user: User): @Cold Single<List<Project>> =
        remoteDataStore
            .loadProjectSummaries(user)
            .timeout(LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS, TimeUnit.SECONDS)

    fun getModifiableLayers(project: Project): ImmutableList<Layer> =
        project.layers
            .filter { !it.userCanAdd.isEmpty() }
            .toImmutableList()

    fun getMutationsOnceAndStream(project: Project): @Cold(terminates = false) Flowable<ImmutableList<Mutation>> {
        return localDataStore.getMutationsOnceAndStream(project)
    }

    fun setCameraPosition(projectId: String, cameraPosition: CameraPosition) =
        localValueStore.setLastCameraPosition(projectId, cameraPosition)

    fun getLastCameraPosition(projectId: String): Optional<CameraPosition> =
        localValueStore.getLastCameraPosition(projectId)
}