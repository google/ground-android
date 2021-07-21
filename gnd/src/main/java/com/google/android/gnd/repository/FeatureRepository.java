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

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation.SyncStatus;
import com.google.android.gnd.model.Mutation.Type;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.room.models.MutationEntitySyncStatus;
import com.google.android.gnd.persistence.remote.NotFoundException;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.sync.DataSyncWorkManager;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.system.auth.AuthenticationManager;
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
 * Coordinates persistence and retrieval of {@link Feature} instances from remote, local, and in
 * memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
public class FeatureRepository {

  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;
  private final ProjectRepository projectRepository;
  private final DataSyncWorkManager dataSyncWorkManager;
  private final AuthenticationManager authManager;
  private final OfflineUuidGenerator uuidGenerator;

  @Inject
  public FeatureRepository(
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore,
      ProjectRepository projectRepository,
      DataSyncWorkManager dataSyncWorkManager,
      AuthenticationManager authManager,
      OfflineUuidGenerator uuidGenerator) {
    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
    this.projectRepository = projectRepository;
    this.dataSyncWorkManager = dataSyncWorkManager;
    this.authManager = authManager;
    this.uuidGenerator = uuidGenerator;
  }

  /**
   * Mirrors features in the specified project from the remote db into the local db when the network
   * is available. When invoked, will first attempt to resync all features from the remote db,
   * subsequently syncing only remote changes. The returned stream never completes, and
   * subscriptions will only terminate on disposal.
   */
  @Cold
  public Completable syncFeatures(Project project) {
    return remoteDataStore
        .loadFeaturesOnceAndStreamChanges(project)
        .flatMapCompletable(this::updateLocalFeature);
  }

  // TODO: Remove "feature" qualifier from this and other repository method names.
  @Cold
  private Completable updateLocalFeature(RemoteDataEvent<Feature> event) {
    switch (event.getEventType()) {
      case ENTITY_LOADED:
      case ENTITY_MODIFIED:
        return event.value().map(localDataStore::mergeFeature).orElse(Completable.complete());
      case ENTITY_REMOVED:
        return localDataStore.deleteFeature(event.getEntityId());
      case ERROR:
        event.error().ifPresent(e -> Timber.d(e, "Invalid features in remote db ignored"));
        return Completable.complete();
      default:
        return Completable.error(
            new UnsupportedOperationException("Event type: " + event.getEventType()));
    }
  }

  // TODO: Only return feature fields needed to render features on map.
  @Cold(terminates = false)
  public Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Project project) {
    return localDataStore.getFeaturesOnceAndStream(project);
  }

  @Cold
  public Single<Feature> getFeature(FeatureMutation featureMutation) {
    return getFeature(featureMutation.getProjectId(), featureMutation.getFeatureId());
  }

  /** This only works if the project and feature are already cached to local db. */
  @Cold
  public Single<Feature> getFeature(String projectId, String featureId) {
    return projectRepository
        .getProject(projectId)
        .flatMapMaybe(project -> localDataStore.getFeature(project, featureId))
        .switchIfEmpty(Single.error(() -> new NotFoundException("Feature not found " + featureId)));
  }

  public FeatureMutation newMutation(String projectId, String layerId, Point point) {
    return FeatureMutation.builder()
        .setType(Type.CREATE)
        .setSyncStatus(SyncStatus.PENDING)
        .setFeatureId(uuidGenerator.generateUuid())
        .setProjectId(projectId)
        .setLayerId(layerId)
        .setNewLocation(Optional.of(point))
        .setUserId(authManager.getCurrentUser().getId())
        .setClientTimestamp(new Date())
        .build();
  }

  public PolygonFeature newPolygonFeature(
      Project project, Layer layer, ImmutableList<Point> point) {
    AuditInfo auditInfo = AuditInfo.now(authManager.getCurrentUser());
    return PolygonFeature.builder()
        .setId(uuidGenerator.generateUuid())
        .setProject(project)
        .setLayer(layer)
        .setVertices(point)
        .setCreated(auditInfo)
        .setLastModified(auditInfo)
        .build();
  }

  /**
   * Creates a mutation entry for the given parameters, applies it to the local db and schedules a
   * task for remote sync if the local transaction is successful.
   *
   * @param mutation Input {@link FeatureMutation}
   * @return If successful, returns the provided feature wrapped as {@link Loadable}
   */
  @Cold
  public Completable applyAndEnqueue(FeatureMutation mutation) {
    Completable localTransaction = localDataStore.applyAndEnqueue(mutation);
    Completable remoteSync = dataSyncWorkManager.enqueueSyncWorker(mutation.getFeatureId());
    return localTransaction.andThen(remoteSync);
  }

  /**
   * Emits the list of {@link FeatureMutation} instances for a given feature which have not yet been
   * marked as {@link SyncStatus#COMPLETED}, including pending, in progress, and failed mutations. A
   * new list is emitted on each subsequent change.
   */
  public Flowable<ImmutableList<FeatureMutation>> getIncompleteFeatureMutationsOnceAndStream(
      String featureId) {
    return localDataStore.getFeatureMutationsByFeatureIdOnceAndStream(
        featureId,
        MutationEntitySyncStatus.PENDING,
        MutationEntitySyncStatus.IN_PROGRESS,
        MutationEntitySyncStatus.FAILED);
  }
}
