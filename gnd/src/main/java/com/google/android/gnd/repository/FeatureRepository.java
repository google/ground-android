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

import com.google.android.gnd.model.Mutation.Type;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.remote.NotFoundException;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.sync.DataSyncWorkManager;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
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

  @Inject
  public FeatureRepository(
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore,
      ProjectRepository projectRepository,
      DataSyncWorkManager dataSyncWorkManager,
      AuthenticationManager authManager) {
    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
    this.projectRepository = projectRepository;
    this.dataSyncWorkManager = dataSyncWorkManager;
    this.authManager = authManager;
  }

  /**
   * Mirrors features in the specified project from the remote db into the local db when the network
   * is available. When invoked, will first attempt to resync all features from the remote db,
   * subsequently syncing only remote changes. The returned stream never completes, and
   * subscriptions will only terminate on disposal.
   */
  public Completable syncFeatures(Project project) {
    return remoteDataStore
        .loadFeaturesOnceAndStreamChanges(project)
        .flatMapCompletable(this::updateLocalFeature);
  }

  // TODO: Remove "feature" qualifier from this and other repository method names.
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
  // TODO(#127): Decouple from Project and accept id instead.
  public Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Project project) {
    return localDataStore.getFeaturesOnceAndStream(project);
  }

  // TODO(#127): Decouple Project from Feature and remove projectId.
  // TODO: Replace with Single and treat missing feature as error.
  // TODO: Don't require projectId to be the active project.
  public Maybe<Feature> getFeature(String projectId, String featureId) {
    return projectRepository
        .getActiveProjectOnceAndStream()
        .compose(Loadable::values)
        .firstElement()
        .filter(project -> project.getId().equals(projectId))
        .switchIfEmpty(Single.error(() -> new NotFoundException("Project " + projectId)))
        .flatMapMaybe(project -> localDataStore.getFeature(project, featureId));
  }

  private FeatureMutation fromFeature(Feature feature, Type type) {
    return FeatureMutation.builder()
        .setType(type)
        .setProjectId(feature.getProject().getId())
        .setFeatureId(feature.getId())
        .setLayerId(feature.getLayer().getId())
        .setNewLocation(Optional.of(feature.getPoint()))
        .setUserId(authManager.getCurrentUser().getId())
        .setClientTimestamp(new Date())
        .build();
  }

  // TODO(#80): Update UI to provide FeatureMutations instead of Features here.
  public Flowable<Loadable<Feature>> createFeature(Feature feature) {
    return applyAndEnqueue(feature, Type.CREATE);
  }

  public Flowable<Loadable<Feature>> updateFeature(Feature feature) {
    return applyAndEnqueue(feature, Type.UPDATE);
  }

  public Flowable<Loadable<Feature>> deleteFeature(Feature feature) {
    return applyAndEnqueue(feature, Type.DELETE);
  }

  private Flowable<Loadable<Feature>> applyAndEnqueue(Feature feature, Type type) {
    return localDataStore
        .applyAndEnqueue(fromFeature(feature, type))
        .andThen(dataSyncWorkManager.enqueueSyncWorker(feature.getId()))
        .toSingleDefault(feature)
        .toFlowable()
        .compose(Loadable::loadingOnceAndWrap);
  }
}
