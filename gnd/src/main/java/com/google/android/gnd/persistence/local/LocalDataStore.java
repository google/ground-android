/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.persistence.local;

import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.basemap.OfflineBaseMap;
import com.google.android.gnd.model.basemap.tile.TileSource;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.persistence.local.room.LocalDataStoreException;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * Provides access to local persistent data store, the canonical store for latest state and
 * mutations queued for sync with remote. Local changes are saved here before being written to the
 * remote data store, and both local and remote updates are always persisted here before being made
 * visible to the user.
 *
 * <p>Implementations are expected to execute each method in this class as a single atomic
 * transaction, and must ensure all subscriptions are run in a background thread (i.e., not the
 * Android main thread).
 *
 * <p>Note that long-lived streams return the full set of entities on each emission rather than
 * deltas to allow changes to not rely on prior UI state (i.e., emissions are idempotent).
 */
public interface LocalDataStore {

  /** Load projects stored in local database. */
  @Cold
  Single<ImmutableList<Project>> getProjects();

  /** Load last active project, if any. */
  @Cold
  Maybe<Project> getProjectById(String id);

  /** Delete stored project from database. */
  @Cold
  Completable deleteProject(Project project);

  /** Add project to the database. */
  @Cold
  Completable insertOrUpdateProject(Project project);

  /** Add user to the database. */
  @Cold
  Completable insertOrUpdateUser(User user);

  /**
   * Loads the user with the specified id from the local data store. The returned Single fails with
   * {@link java.util.NoSuchElementException} if not found.
   */
  @Cold
  Single<User> getUser(String id);

  /**
   * Applies the specified {@link FeatureMutation} to the local data store, appending the mutation
   * to the local queue for remote sync.
   */
  @Cold
  Completable applyAndEnqueue(FeatureMutation mutation);

  /**
   * Applies the specified {@link ObservationMutation} to the local data store, appending the
   * mutation to the local queue for remote sync.
   */
  @Cold
  Completable applyAndEnqueue(ObservationMutation mutation);

  /** Applies the specified {@link ObservationMutation} to the local data store.. */
  @Cold
  Completable apply(ObservationMutation mutation) throws LocalDataStoreException;

  /**
   * Returns a long-lived stream that emits the full set of features for a project on subscribe, and
   * continues to return the full set each time a feature is added/changed/removed.
   */
  @Cold(terminates = false)
  Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Project project);

  /**
   * Returns the list of observations which are not marked for deletion for the specified feature
   * and form.
   */
  @Cold
  Single<ImmutableList<Observation>> getObservations(Feature feature, String formId);

  /** Returns the feature with the specified UUID from the local data store, if found. */
  @Cold
  Maybe<Feature> getFeature(Project project, String featureId);

  /** Returns the observation with the specified UUID from the local data store, if found. */
  @Cold
  Maybe<Observation> getObservation(Feature feature, String observationId);

  /**
   * Returns a long-lived stream that emits the full set of tiles on subscribe and continues to
   * return the full set each time a tile is added/changed/removed.
   */
  @Cold(terminates = false)
  Flowable<ImmutableSet<TileSource>> getTileSourcesOnceAndStream();

  /**
   * Returns all feature and observation mutations in the local mutation queue relating to feature
   * with the specified id.
   */
  @Cold
  Single<ImmutableList<Mutation>> getPendingMutations(String featureId);

  /** Updates the provided list of mutations. */
  @Cold
  Completable updateMutations(ImmutableList<Mutation> mutations);

  /**
   * Removes pending mutations and if the mutation is of type DELETE, then removes the corresponding
   * observation or feature.
   */
  @Cold
  Completable finalizePendingMutations(ImmutableList<Mutation> mutations);

  /**
   * Merges the provided feature with pending unsynced local mutations, and inserts it into the
   * local data store. If a feature with the same id already exists, it will be overwritten with the
   * merged instance.
   */
  @Cold
  Completable mergeFeature(Feature feature);

  /** Deletes feature from local database. */
  @Cold
  Completable deleteFeature(String featureId);

  /**
   * Merges the provided observation with pending unsynced local mutations, and inserts it into the
   * local data store. If a observation with the same id already exists, it will be overwritten with
   * the merged instance.
   */
  @Cold
  Completable mergeObservation(Observation observation);

  /** Deletes observation from local database. */
  @Cold
  Completable deleteObservation(String observationId);

  /**
   * Attempts to update a tile in the local data store. If the tile doesn't exist, inserts the tile
   * into the local data store.
   */
  @Cold
  Completable insertOrUpdateTileSource(TileSource tileSource);

  /** Returns the tile with the specified URL from the local data store, if found. */
  @Cold
  Maybe<TileSource> getTileSource(String tileUrl);

  /** Returns all pending tiles from the local data store. */
  @Cold
  Single<ImmutableList<TileSource>> getPendingTileSources();

  /**
   * Attempts to update an offline area in the local data store. If the area doesn't exist, inserts
   * the area into the local data store.
   */
  @Cold
  Completable insertOrUpdateOfflineArea(OfflineBaseMap area);

  /** Returns all queued, failed, and completed offline areas from the local data store. */
  @Cold(terminates = false)
  Flowable<ImmutableList<OfflineBaseMap>> getOfflineAreasOnceAndStream();

  /** Delete an offline area and any associated tiles that are no longer needed. */
  @Cold
  Completable deleteOfflineArea(String offlineAreaId);

  /** Returns the offline area with the specified id. */
  Single<OfflineBaseMap> getOfflineAreaById(String id);

  /**
   * Update the area count of an existing tile source in the local data store with the area count of
   * {@param tilesource}.
   */
  @Cold
  Completable updateTileSourceBasemapReferenceCountByUrl(int newCount, String url);

  /** Delete a tile source associated with a given URL from the local data store. */
  @Cold
  Completable deleteTileByUrl(TileSource tile);
}
