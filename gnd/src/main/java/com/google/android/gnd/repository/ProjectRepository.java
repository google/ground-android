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

package com.google.android.gnd.repository;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.Role;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.persistence.remote.NotFoundException;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.map.CameraPosition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Coordinates persistence and retrieval of {@link Project} instances from remote, local, and in
 * memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
public class ProjectRepository {

  private static final long LOAD_REMOTE_PROJECT_TIMEOUT_SECS = 15;
  private static final long LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS = 30;

  private final UserRepository userRepository;
  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final LocalValueStore localValueStore;

  /** Emits a project id on {@see #activateProject} and empty on {@see #clearActiveProject}. */
  @Hot
  private final FlowableProcessor<Optional<String>> selectProjectEvent = PublishProcessor.create();

  /** Emits the latest loading state of the current project on subscribe and on change. */
  @Hot(replays = true)
  private final FlowableProcessor<Loadable<Project>> projectLoadingState =
      BehaviorProcessor.create();

  @Inject
  public ProjectRepository(
      UserRepository userRepository,
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore,
      LocalValueStore localValueStore) {
    this.userRepository = userRepository;
    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
    this.localValueStore = localValueStore;

    // Kicks off the loading process whenever a new project id is selected.
    selectProjectEvent
        .distinctUntilChanged()
        .switchMap(this::activateProject)
        .onBackpressureLatest()
        .subscribe(projectLoadingState);
  }

  @Cold
  private Flowable<Loadable<Project>> activateProject(Optional<String> projectId) {
    // Empty id indicates intent to deactivate the current project. Used on sign out.
    if (projectId.isEmpty()) {
      return Flowable.just(Loadable.notLoaded());
    }
    String id = projectId.get();
    return syncProjectWithRemote(id)
        .onErrorResumeNext(__ -> getProject(id))
        .map(this::attachLayerPermissions)
        .doOnSuccess(__ -> localValueStore.setLastActiveProjectId(id))
        .toFlowable()
        .compose(Loadable::loadingOnceAndWrap);
  }

  private Project attachLayerPermissions(Project project) {
    Role userRole = userRepository.getUserRole(project);
    ImmutableMap.Builder layers = ImmutableMap.builder();
    for (Layer layer : project.getLayers()) {
      layers.put(
          layer.getId(),
          layer.toBuilder().setUserCanAdd(getAddableFeatureTypes(userRole, layer)).build());
    }
    return project.toBuilder().setLayerMap(layers.build()).build();
  }

  private ImmutableList<FeatureType> getAddableFeatureTypes(Role userRole, Layer layer) {
    switch (userRole) {
      case OWNER:
      case MANAGER:
        return FeatureType.ALL;
      case CONTRIBUTOR:
        return layer.getContributorsCanAdd();
      case UNKNOWN:
      default:
        return ImmutableList.of();
    }
  }

  /** This only works if the project is already cached to local db. */
  @Cold
  public Single<Project> getProject(String projectId) {
    return localDataStore
        .getProjectById(projectId)
        .switchIfEmpty(Single.error(() -> new NotFoundException("Project not found " + projectId)));
  }

  @Cold
  private Single<Project> syncProjectWithRemote(String id) {
    return remoteDataStore
        .loadProject(id)
        .timeout(LOAD_REMOTE_PROJECT_TIMEOUT_SECS, TimeUnit.SECONDS)
        .flatMap(p -> localDataStore.insertOrUpdateProject(p).toSingleDefault(p))
        .doOnSubscribe(__ -> Timber.d("Loading project %s", id))
        .doOnError(err -> Timber.d(err, "Error loading project from remote"));
  }

  public Optional<String> getLastActiveProjectId() {
    return Optional.ofNullable(localValueStore.getLastActiveProjectId());
  }

  /**
   * Returns an observable that emits the latest project activation state, and continues to emit
   * changes to that state until all subscriptions are disposed.
   */
  @Hot(replays = true)
  public Flowable<Loadable<Project>> getProjectLoadingState() {
    return projectLoadingState;
  }

  @Hot(replays = true)
  public Flowable<Optional<Project>> getActiveProject() {
    return projectLoadingState.map(Loadable::value);
  }

  public void activateProject(String projectId) {
    Timber.v("activateProject() called with %s", projectId);
    selectProjectEvent.onNext(Optional.of(projectId));
  }

  @Cold
  public Flowable<Loadable<List<Project>>> getProjectSummaries(User user) {
    return loadProjectSummariesFromRemote(user)
        .doOnSubscribe(__ -> Timber.d("Loading project list from remote"))
        .doOnError(err -> Timber.d(err, "Failed to load project list from remote"))
        .onErrorResumeNext(__ -> localDataStore.getProjects())
        .toFlowable()
        .compose(Loadable::loadingOnceAndWrap);
  }

  @Cold
  public Single<ImmutableList<Project>> getOfflineProjects() {
    return localDataStore.getProjects();
  }

  @Cold
  private Single<List<Project>> loadProjectSummariesFromRemote(User user) {
    return remoteDataStore
        .loadProjectSummaries(user)
        .timeout(LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS, TimeUnit.SECONDS);
  }

  /** Clears the currently active project from cache. */
  public void clearActiveProject() {
    selectProjectEvent.onNext(Optional.empty());
  }

  public ImmutableList<Layer> getModifiableLayers(Project project) {
    return stream(project.getLayers())
        .filter(layer -> !layer.getUserCanAdd().isEmpty())
        .collect(toImmutableList());
  }

  public Flowable<ImmutableList<Mutation>> getMutationsOnceAndStream(Project project) {
    return localDataStore.getMutationsOnceAndStream(project);
  }

  public void setCameraPosition(String projectId, CameraPosition cameraPosition) {
    localValueStore.setLastCameraPosition(projectId, cameraPosition);
  }

  public Optional<CameraPosition> getLastCameraPosition(String projectId) {
    return localValueStore.getLastCameraPosition(projectId);
  }
}
