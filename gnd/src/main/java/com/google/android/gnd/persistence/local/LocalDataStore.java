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
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;

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
  Single<List<Project>> getProjects();

  /** Load last active project, if any. */
  Maybe<Project> getProjectById(String id);

  /** Delete stored project from database. */
  Completable removeProject(Project project);

  /** Add project to the database. */
  Completable insertOrUpdateProject(Project project);

  /**
   * Applies the specified {@link FeatureMutation} to the local data store, appending the mutation
   * to the local queue for remote sync.
   */
  Completable applyAndEnqueue(FeatureMutation mutation);

  /**
   * Applies the specified {@link ObservationMutation} to the local data store, appending the
   * mutation to the local queue for remote sync.
   */
  Completable applyAndEnqueue(ObservationMutation mutation);

  /**
   * Returns a long-lived stream that emits the full set of features for a project on subscribe, and
   * continues to return the full set each time a feature is added/changed/removed.
   */
  Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Project project);

  /** Returns the full list of records for the specified feature and form. */
  Single<ImmutableList<Observation>> getRecords(Feature feature, String formId);

  /** Returns the feature with the specified UUID from the local data store, if found. */
  Maybe<Feature> getFeature(Project project, String featureId);

  /** Returns the observation with the specified UUID from the local data store, if found. */
  Maybe<Observation> getRecord(Feature feature, String recordId);

  /**
   * Returns a long-lived stream that emits the full set of tiles on subscribe and continues to
   * return the full set each time a tile is added/changed/removed.
   */
  Flowable<ImmutableSet<Tile>> getTilesOnceAndStream();

  /**
   * Returns all feature and observation mutations in the local mutation queue relating to feature
   * with the specified id.
   */
  Single<ImmutableList<Mutation>> getPendingMutations(String featureId);

  /** Updates the provided list of mutations. */
  Completable updateMutations(ImmutableList<Mutation> mutations);

  /** Removes the provided feature and observation mutations from the local mutation queue. */
  Completable removePendingMutations(ImmutableList<Mutation> mutations);

  /**
   * Merges the provided feature with pending unsynced local mutations, and inserts it into the
   * local data store. If a feature with the same id already exists, it will be overwritten with the
   * merged instance.
   */
  Completable mergeFeature(Feature feature);

  /**
   * Merges the provided observation with pending unsynced local mutations, and inserts it into the
   * local data store. If a observation with the same id already exists, it will be overwritten with
   * the merged instance.
   */
  Completable mergeRecord(Observation observation);

  /**
   * Attempts to update a tile in the local data store. If the tile doesn't exist, inserts the tile
   * into the local data store.
   */
  Completable insertOrUpdateTile(Tile tile);

  /** Returns the tile with the specified id from the local data store, if found. */
  Maybe<Tile> getTile(String tileId);
}
