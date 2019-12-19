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
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.firebase.firestore.FirebaseFirestoreException;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataRepository {
  private static final String TAG = DataRepository.class.getSimpleName();
  private static final long GET_REMOTE_RECORDS_TIMEOUT_SECS = 5;

  private final InMemoryCache cache;
  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final DataSyncWorkManager dataSyncWorkManager;
  private final Flowable<Loadable<Project>> activeProject;
  private final FlowableProcessor<String> activateProjectRequests;
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

    this.activateProjectRequests = PublishProcessor.create();

    this.activeProject = activateProjectRequests.switchMap(id -> loadProject(id));

    // Last active project will be loaded once view subscribes to activeProject.
    getLastActiveProjectId().ifPresent(activateProjectRequests::onNext);

    streamFeaturesToLocalDb(remoteDataStore);
  }

  private Flowable<Loadable<Project>> loadProject(String id) {
    return remoteDataStore
        .loadProject(id)
        .doOnSuccess(__ -> localValueStore.setLastActiveProjectId(id))
        .toFlowable()
        .compose(Loadable::wrapValueStream);
    // TODO: Write to local DB
    // TODO: Fall back to local DB on error
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
    activeProject
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
    return activeProject;
  }

  public void activateProject(String projectId) {
    activateProjectRequests.onNext(projectId);
  }
  /*
    private Single<Project> activateProjectInternal(String projectId) {
      Log.d(TAG, " Activating project " + projectId);
      return remoteDataStore
          .loadProject(projectId)
          .flatMap(project -> localDataStore.insertOrUpdateProject(project).toSingleDefault(project))
          .onErrorResumeNext(
              throwable -> {
                if (throwable instanceof FirebaseFirestoreException) {
                  return localDataStore
                      .getProjectById(localValueStore.getLastActiveProjectId())
                      .toSingle();
                }
                return Single.error(throwable);
              })
          .doOnError(throwable -> Log.e(TAG, "Project not found " + projectId))
          .doOnSubscribe(__ -> activeProject.onNext(Loadable.loading()))
          .flatMap(project -> localDataStore.insertOrUpdateProject(project).toSingleDefault(project))
          .doOnSuccess(this::onProjectLoaded);
    }

    private void onProjectLoaded(Project project) {
      cache.setActiveProject(project);
      activeProject.onNext(Loadable.loaded(project));
      localValueStore.setLastActiveProjectId(project.getId());
    }
  */
  public Observable<Loadable<List<Project>>> getProjectSummaries(User user) {
    // TODO: Get from load db if network connection not available or remote times out.
    return remoteDataStore
        .loadProjectSummaries(user)
        .onErrorResumeNext(
            throwable -> {
              if (throwable instanceof FirebaseFirestoreException) {
                return localDataStore.getProjects();
              }
              return Single.error(throwable);
            })
        .map(Loadable::loaded)
        .onErrorReturn(Loadable::error)
        .toObservable()
        .startWith(Loadable.loading());
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
            .timeout(GET_REMOTE_RECORDS_TIMEOUT_SECS, TimeUnit.SECONDS)
            .doOnError(t -> Log.d(TAG, "Observation sync timed out"))
            .flatMapCompletable(this::mergeRemoteRecords)
            .onErrorComplete();
    return remoteSync.andThen(localDataStore.getRecords(feature, formId));
  }

  private Completable mergeRemoteRecords(ImmutableList<Observation> observations) {
    return Observable.fromIterable(observations).flatMapCompletable(localDataStore::mergeRecord);
  }

  // TODO(#127): Decouple Project from Feature and remove projectId.
  // TODO: Replace with Single and treat missing id as error.
  private Maybe<Feature> getFeature(String projectId, String featureId) {
    return getProject(projectId)
        .flatMapMaybe(project -> localDataStore.getFeature(project, featureId));
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

  private Single<Project> getProject(String projectId) {
    // TODO: Try to load from db if network not available or times out.
    return Maybe.fromCallable(cache::getActiveProject)
        .filter(p -> projectId.equals(p.getId()))
        .switchIfEmpty(remoteDataStore.loadProject(projectId));
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
    // TODO: We plan to allow multiple users, in which case we wouldn't want to delete projects
    // from the local store on sign out.
    //    localDataStore
    //        .getProjectById(localValueStore.getLastActiveProjectId())
    //        .flatMapCompletable(localDataStore::removeProject)
    //        .subscribe();

    // TODO: Now that we use the local db should we get rid of the project (and even features?) in
    // the cache? If not we'll need to implement logic to keep the cache in sync with the local and
    // remote dbs.
    cache.clearActiveProject();
    localValueStore.clearLastActiveProjectId();
    // TODO: Pass special value through activateProjectRequests stream to trigger
    // Loadable.notLoaded()?
    //    activeProject.onNext(Loadable.notLoaded());
  }
}
