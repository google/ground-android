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

package com.google.android.ground.repository;

import com.google.android.ground.model.Survey;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.locationofinterest.Point;
import com.google.android.ground.model.mutation.LocationOfInterestMutation;
import com.google.android.ground.model.mutation.Mutation.SyncStatus;
import com.google.android.ground.model.mutation.Mutation.Type;
import com.google.android.ground.persistence.local.LocalDataStore;
import com.google.android.ground.persistence.local.LocalValueStore;
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus;
import com.google.android.ground.persistence.remote.NotFoundException;
import com.google.android.ground.persistence.remote.RemoteDataEvent;
import com.google.android.ground.persistence.remote.RemoteDataStore;
import com.google.android.ground.persistence.sync.DataSyncWorkManager;
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator;
import com.google.android.ground.rx.Loadable;
import com.google.android.ground.rx.annotations.Cold;
import com.google.android.ground.system.auth.AuthenticationManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.Date;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Coordinates persistence and retrieval of {@link LocationOfInterest} instances from remote, local,
 * and in memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
public class LocationOfInterestRepository {

  private final LocalDataStore localDataStore;
  private final LocalValueStore localValueStore;
  private final RemoteDataStore remoteDataStore;
  private final SurveyRepository surveyRepository;
  private final DataSyncWorkManager dataSyncWorkManager;
  private final AuthenticationManager authManager;
  private final OfflineUuidGenerator uuidGenerator;

  @Inject
  public LocationOfInterestRepository(
      LocalDataStore localDataStore,
      LocalValueStore localValueStore,
      RemoteDataStore remoteDataStore,
      SurveyRepository surveyRepository,
      DataSyncWorkManager dataSyncWorkManager,
      AuthenticationManager authManager,
      OfflineUuidGenerator uuidGenerator) {
    this.localDataStore = localDataStore;
    this.localValueStore = localValueStore;
    this.remoteDataStore = remoteDataStore;
    this.surveyRepository = surveyRepository;
    this.dataSyncWorkManager = dataSyncWorkManager;
    this.authManager = authManager;
    this.uuidGenerator = uuidGenerator;
  }

  /**
   * Mirrors locations of interest in the specified survey from the remote db into the local db when
   * the network is available. When invoked, will first attempt to resync all locations of interest
   * from the remote db, subsequently syncing only remote changes. The returned stream never
   * completes, and subscriptions will only terminate on disposal.
   */
  @Cold
  public Completable syncLocationsOfInterest(Survey survey) {
    return remoteDataStore
        .loadLocationsOfInterestOnceAndStreamChanges(survey)
        .flatMapCompletable(this::updateLocalLocationOfInterest);
  }

  // TODO: Remove "location of interest" qualifier from this and other repository method names.
  @Cold
  private Completable updateLocalLocationOfInterest(RemoteDataEvent<LocationOfInterest> event) {
    switch (event.getEventType()) {
      case ENTITY_LOADED:
      case ENTITY_MODIFIED:
        return event
            .value()
            .map(localDataStore::mergeLocationOfInterest)
            .orElse(Completable.complete());
      case ENTITY_REMOVED:
        return localDataStore.deleteLocationOfInterest(event.getEntityId());
      case ERROR:
        event
            .error()
            .ifPresent(e -> Timber.d(e, "Invalid locations of interest in remote db ignored"));
        return Completable.complete();
      default:
        return Completable.error(
            new UnsupportedOperationException("Event type: " + event.getEventType()));
    }
  }

  // TODO: Only return location of interest fields needed to render locations of interest on map.
  @Cold(terminates = false)
  public Flowable<ImmutableSet<LocationOfInterest>> getLocationsOfInterestOnceAndStream(
      Survey survey) {
    return localDataStore.getLocationsOfInterestOnceAndStream(survey);
  }

  @Cold
  public Single<LocationOfInterest> getLocationOfInterest(
      LocationOfInterestMutation locationOfInterestMutation) {
    return this.getLocationOfInterest(
        locationOfInterestMutation.getSurveyId(),
        locationOfInterestMutation.getLocationOfInterestId());
  }

  /** This only works if the survey and location of interests are already cached to local db. */
  @Cold
  public Single<LocationOfInterest> getLocationOfInterest(
      String surveyId, String locationOfInterest) {
    return surveyRepository
        .getSurvey(surveyId)
        .flatMapMaybe(survey -> localDataStore.getLocationOfInterest(survey, locationOfInterest))
        .switchIfEmpty(
            Single.error(
                () ->
                    new NotFoundException("Location of interest not found " + locationOfInterest)));
  }

  public LocationOfInterestMutation newMutation(
      String surveyId, String jobId, Point point, Date date) {
    return LocationOfInterestMutation.builder()
        .setLocation(Optional.of(point))
        .setType(Type.CREATE)
        .setSyncStatus(SyncStatus.PENDING)
        .setLocationOfInterestId(uuidGenerator.generateUuid())
        .setSurveyId(surveyId)
        .setJobId(jobId)
        .setUserId(authManager.getCurrentUser().getId())
        .setClientTimestamp(date)
        .build();
  }

  public LocationOfInterestMutation newPolygonOfInterestMutation(
      String surveyId, String jobId, ImmutableList<Point> vertices, Date date) {
    return LocationOfInterestMutation.builder()
        .setPolygonVertices(vertices)
        .setType(Type.CREATE)
        .setSyncStatus(SyncStatus.PENDING)
        .setLocationOfInterestId(uuidGenerator.generateUuid())
        .setSurveyId(surveyId)
        .setJobId(jobId)
        .setUserId(authManager.getCurrentUser().getId())
        .setClientTimestamp(date)
        .build();
  }

  /**
   * Creates a mutation entry for the given parameters, applies it to the local db and schedules a
   * task for remote sync if the local transaction is successful.
   *
   * @param mutation Input {@link LocationOfInterestMutation}
   * @return If successful, returns the provided locations of interest wrapped as {@link Loadable}
   */
  @Cold
  public Completable applyAndEnqueue(LocationOfInterestMutation mutation) {
    Completable localTransaction = localDataStore.applyAndEnqueue(mutation);
    Completable remoteSync =
        dataSyncWorkManager.enqueueSyncWorker(mutation.getLocationOfInterestId());
    return localTransaction.andThen(remoteSync);
  }

  /**
   * Emits the list of {@link LocationOfInterestMutation} instances for a given location of interest
   * which have not yet been marked as {@link SyncStatus#COMPLETED}, including pending, in progress,
   * and failed mutations. A new list is emitted on each subsequent change.
   */
  public Flowable<ImmutableList<LocationOfInterestMutation>>
      getIncompleteLocationOfInterestMutationsOnceAndStream(String locationOfInterestId) {
    return localDataStore.getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
        locationOfInterestId,
        MutationEntitySyncStatus.PENDING,
        MutationEntitySyncStatus.IN_PROGRESS,
        MutationEntitySyncStatus.FAILED);
  }

  public boolean isPolygonDialogInfoShown() {
    return localValueStore.isPolygonDialogInfoShown();
  }

  public void setPolygonDialogInfoShown(boolean value) {
    localValueStore.setPolygonInfoDialogShown(value);
  }
}
