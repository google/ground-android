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

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.rx.Loadable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Coordinates persistence and retrieval of {@link Project} instances from remote, local, and in
 * memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
public class ProjectRepository {
  private static final String TAG = ProjectRepository.class.getSimpleName();
  private static final long LOAD_REMOTE_PROJECT_TIMEOUT_SECS = 5;
  private static final long LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS = 10;

  private final InMemoryCache cache;
  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final Flowable<Loadable<Project>> activeProjectStream;
  private final FlowableProcessor<Optional<String>> activateProjectRequests;
  private final LocalValueStore localValueStore;

  @Inject
  public ProjectRepository(
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore,
      InMemoryCache cache,
      LocalValueStore localValueStore) {
    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
    this.cache = cache;
    this.localValueStore = localValueStore;

    // BehaviorProcessor re-emits last requested project id to late subscribers.
    this.activateProjectRequests = BehaviorProcessor.create();

    // Load project when requested id changes, caching the last loaded project.
    this.activeProjectStream =
        activateProjectRequests
            .distinctUntilChanged()
            .doOnNext(id -> Log.v(TAG, "Requested project id changed: " + id))
            .switchMap(this::onActivateProjectRequest)
            .replay(1)
            .refCount();
  }

  private Flowable<Loadable<Project>> onActivateProjectRequest(Optional<String> projectId) {
    // Empty id indicates intent to deactivate the current project. Used on sign out.
    if (projectId.isEmpty()) {
      return Flowable.just(Loadable.notLoaded());
    }
    String id = projectId.get();

    return syncProjectWithRemote(id)
        .doOnSubscribe(__ -> Log.d(TAG, "Activating project " + id))
        .doOnError(err -> Log.d(TAG, "Error loading project from remote", err))
        .onErrorResumeNext(
            __ ->
                localDataStore
                    .getProjectById(id)
                    .toSingle()
                    .doOnError(err -> Log.d(TAG, "Error loading project from local db", err)))
        .doOnSuccess(__ -> localValueStore.setLastActiveProjectId(id))
        .toFlowable()
        .compose(Loadable::loadingOnceAndWrap);
  }

  private Single<Project> syncProjectWithRemote(String id) {
    return remoteDataStore
        .loadProject(id)
        .timeout(LOAD_REMOTE_PROJECT_TIMEOUT_SECS, TimeUnit.SECONDS)
        .flatMap(p -> localDataStore.insertOrUpdateProject(p).toSingleDefault(p));
  }

  @NonNull
  public Optional<String> getLastActiveProjectId() {
    return Optional.ofNullable(localValueStore.getLastActiveProjectId());
  }

  /**
   * Returns a stream that emits the latest project activation state, and continues to emits changes
   * to that state until all subscriptions are disposed.
   */
  public Flowable<Loadable<Project>> getActiveProjectOnceAndStream() {
    return activeProjectStream;
  }

  public void activateProject(String projectId) {
    Log.v(TAG, "activateProject() called with " + projectId);
    activateProjectRequests.onNext(Optional.of(projectId));
  }

  public Flowable<Loadable<List<Project>>> getProjectSummaries(User user) {
    return loadProjectSummariesFromRemote(user)
        .doOnSubscribe(__ -> Log.d(TAG, "Loading project list from remote"))
        .doOnError(err -> Log.d(TAG, "Failed to load project list from remote", err))
        .onErrorResumeNext(__ -> localDataStore.getProjects())
        .toFlowable()
        .compose(Loadable::loadingOnceAndWrap);
  }

  public Single<List<Project>> getOfflineProjects() {
    return localDataStore.getProjects();
  }

  private Single<List<Project>> loadProjectSummariesFromRemote(User user) {
    return remoteDataStore
        .loadProjectSummaries(user)
        .timeout(LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS, TimeUnit.SECONDS);
  }

  /** Clears the currently active project from cache and from local localValueStore. */
  public void clearActiveProject() {
    cache.clear();
    localValueStore.clearLastActiveProjectId();
    activateProjectRequests.onNext(Optional.empty());
  }
}
