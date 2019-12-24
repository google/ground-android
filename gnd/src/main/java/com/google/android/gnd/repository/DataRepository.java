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
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.firestore.DocumentNotFoundException;
import com.google.android.gnd.persistence.sync.DataSyncWorkManager;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.rx.Result;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataRepository {
  private static final String TAG = DataRepository.class.getSimpleName();
  private static final long LOAD_REMOTE_RECORDS_TIMEOUT_SECS = 5;
  private static final long LOAD_REMOTE_PROJECT_TIMEOUT_SECS = 5;

  private final InMemoryCache cache;
  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final DataSyncWorkManager dataSyncWorkManager;
  private final Flowable<Loadable<Project>> activeProjectStream;
  private final FlowableProcessor<Optional<String>> activateProjectRequests;
  private final OfflineUuidGenerator uuidGenerator;
  private final LocalValueStore localValueStore;

  @Inject
  public DataRepository(
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore,
      DataSyncWorkManager dataSyncWorkManager,
      InMemoryCache cache,
      OfflineUuidGenerator uuidGenerator,
      LocalValueStore localValueStore) {
    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
    this.dataSyncWorkManager = dataSyncWorkManager;
    this.cache = cache;
    this.uuidGenerator = uuidGenerator;
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
    streamFeaturesToLocalDb(remoteDataStore);
  }

  private Flowable<Loadable<Project>> onActivateProjectRequest(Optional<String> projectId) {
    // Empty id indicates intent to deactivate the current project. Used. on sign out.
    if (projectId.isEmpty()) {
      return Flowable.just(Loadable.notLoaded());
    }
    String id = projectId.get();

    return syncProjectWithRemote(id)
        .doOnSubscribe(__ -> Log.i(TAG, "Activating project " + id))
        .toFlowable()
        .compose(Result::wrap)
        .compose(Result.onErrorResumeNext(loadProjectFromLocal(id)))
        .doOnNext(__ -> localValueStore.setLastActiveProjectId(id))
        .compose(Result::unwrap)
        .compose(Loadable::loadingOnceAndResults);
  }

  private Flowable<Result<Project>> loadProjectFromLocal(String id) {
    // getProjectById() is lazy, so the project will only be loaded from the local data store as
    // needed (i.e., on subscribe when fetch from remote data store fails).
    return localDataStore
        .getProjectById(id)
        .doOnSubscribe(__ -> Log.i(TAG, "Falling back to local db"))
        .toFlowable()
        .compose(Result::wrap);
  }

  private Single<Project> syncProjectWithRemote(String id) {
    return remoteDataStore
        .loadProject(id)
        .timeout(LOAD_REMOTE_PROJECT_TIMEOUT_SECS, TimeUnit.SECONDS)
        .doOnSuccess(localDataStore::insertOrUpdateProject);
  }

  @NonNull
  public Optional<String> getLastActiveProjectId() {
    return Optional.ofNullable(localValueStore.getLastActiveProjectId());
  }

  /**
   * Mirrors features in the current project from the remote db into the local db when the network
   * is available. When invoked, will first attempt to resync all features from the remote db,
   * subsequently syncing only remote changes.
   */
  private void streamFeaturesToLocalDb(RemoteDataStore remoteDataStore) {
    // TODO: Move to Application or background service.
    // TODO: Is this even working? If the returned Disposable is garbage collected this will be
    // interrupted.
    activeProjectStream
        .compose(Loadable::values)
        .switchMap(remoteDataStore::loadFeaturesOnceAndStreamChanges)
        .switchMap(event -> updateLocalFeature(event).toFlowable())
        .subscribe();
  }

  private Completable updateLocalFeature(RemoteDataEvent<Feature> event) {
    switch (event.getEventType()) {
      case ENTITY_LOADED:
      case ENTITY_MODIFIED:
        return event.value().map(localDataStore::mergeFeature).orElse(Completable.complete());
      case ENTITY_REMOVED:
        // TODO: Delete features:
        // localDataStore.removeFeature(event.getEntityId());
        return Completable.complete();
      case ERROR:
        return Completable.error(event.error().get());
      default:
        return Completable.error(
            new UnsupportedOperationException("Event type: " + event.getEventType()));
    }
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
    return remoteDataStore
        .loadProjectSummaries(user)
        .toFlowable()
        .compose(Result::wrap)
        .compose(Result.onErrorResumeNext(loadProjectSummariesFromLocal()))
        .compose(Result::unwrap)
        .compose(Loadable::loadingOnceAndResults);
  }

  private Flowable<Result<List<Project>>> loadProjectSummariesFromLocal() {
    return localDataStore.getProjects().toFlowable().compose(Result::wrap);
  }

  // TODO: Only return feature fields needed to render features on map.
  // TODO(#127): Decouple from Project and accept id instead.
  public Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Project project) {
    return localDataStore.getFeaturesOnceAndStream(project);
  }

  /**
   * Retrieves the records or the specified project, feature, and form.
   *
   * <ol>
   *   <li>Attempt to sync remote observation changes to the local data store. If network is not
   *       available or operation times out, this step is skipped.
   *   <li>Relevant records are returned directly from the local data store.
   * </ol>
   */
  public Single<ImmutableList<Observation>> getRecords(
      String projectId, String featureId, String formId) {
    // TODO: Only fetch first n fields.
    // TODO(#127): Decouple feature from observation so that we don't need to fetch observation
    // here.
    return getFeature(projectId, featureId)
        .switchIfEmpty(Single.error(new DocumentNotFoundException()))
        .flatMap(feature -> getRecords(feature, formId));
  }

  private Single<ImmutableList<Observation>> getRecords(Feature feature, String formId) {
    Completable remoteSync =
        remoteDataStore
            .loadRecords(feature)
            .timeout(LOAD_REMOTE_RECORDS_TIMEOUT_SECS, TimeUnit.SECONDS)
            .doOnError(t -> Log.d(TAG, "Observation sync timed out"))
            .flatMapCompletable(this::mergeRemoteRecords)
            .onErrorComplete();
    return remoteSync.andThen(localDataStore.getRecords(feature, formId));
  }

  private Completable mergeRemoteRecords(ImmutableList<Observation> observations) {
    return Observable.fromIterable(observations).flatMapCompletable(localDataStore::mergeRecord);
  }

  // TODO(#127): Decouple Project from Feature and remove projectId.
  // TODO: Replace with Single and treat missing feature as error.
  private Maybe<Feature> getFeature(String projectId, String featureId) {
    return activeProjectStream
        .compose(Loadable::values)
        .firstElement()
        .filter(project -> project.getId().equals(projectId))
        .flatMap(project -> localDataStore.getFeature(project, featureId));
  }

  public Single<Observation> getObservation(
      String projectId, String featureId, String observationId) {
    // TODO: Store and retrieve latest edits from cache and/or db.
    // TODO(#127): Decouple feature from observation so that we don't need to fetch feature here.
    return getFeature(projectId, featureId)
        .switchIfEmpty(Single.error(new DocumentNotFoundException()))
        .flatMap(
            feature ->
                localDataStore
                    .getRecord(feature, observationId)
                    .switchIfEmpty(Single.error(new DocumentNotFoundException())));
  }

  public Single<Observation> createObservation(String projectId, String featureId, String formId) {
    // TODO: Handle invalid formId.
    // TODO(#127): Decouple feature from observation so that we don't need to fetch feature here.
    return getFeature(projectId, featureId)
        .switchIfEmpty(Single.error(new DocumentNotFoundException()))
        .map(
            feature ->
                Observation.newBuilder()
                    .setId(uuidGenerator.generateUuid())
                    .setProject(feature.getProject())
                    .setFeature(feature)
                    .setForm(feature.getLayer().getForm(formId).get())
                    .build());
  }

  public Completable applyAndEnqueue(ObservationMutation mutation) {
    // TODO(#101): Store user id and timestamp on save.
    return localDataStore
        .applyAndEnqueue(mutation)
        .andThen(dataSyncWorkManager.enqueueSyncWorker(mutation.getFeatureId()));
  }

  public Completable saveFeature(Feature feature) {
    // TODO(#79): Assign owner and timestamps when creating new feature.
    // TODO(#80): Update UI to provide FeatureMutations instead of Features here.
    return localDataStore
        .applyAndEnqueue(
            FeatureMutation.builder()
                .setType(Mutation.Type.CREATE)
                .setProjectId(feature.getProject().getId())
                .setFeatureId(feature.getId())
                .setLayerId(feature.getLayer().getId())
                .setNewLocation(Optional.of(feature.getPoint()))
                // TODO(#101): Attach real credentials.
                .setUserId("")
                .build())
        .andThen(dataSyncWorkManager.enqueueSyncWorker(feature.getId()));
  }

  /** Clears the currently active project from cache and from local localValueStore. */
  public void clearActiveProject() {
    cache.clear();
    localValueStore.clearLastActiveProjectId();
    activateProjectRequests.onNext(Optional.empty());
  }
}
