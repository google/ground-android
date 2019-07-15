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
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.observation.Record;
import com.google.android.gnd.model.observation.RecordMutation;
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
 * <p>Note that long-lived streams return the full set of entities on each emission rather than
 * deltas to allow changes to not rely on prior UI state (i.e., emissions are idempotent).
 */
public interface LocalDataStore {
  /**
   * Applies the specified {@link FeatureMutation} to the local data store, appending the mutation
   * to the local queue for remote sync.
   */
  Completable applyAndEnqueue(FeatureMutation mutation);

  /**
   * Applies the specified {@link RecordMutation} to the local data store, appending the mutation to
   * the local queue for remote sync.
   */
  Completable applyAndEnqueue(RecordMutation mutation);

  /**
   * Returns a long-lived stream that emits the full set of features for a project on subscribe, and
   * continues to return the full set each time a feature is added/changed/removed.
   */
  Flowable<ImmutableSet<Feature>> getFeaturesOnceAndStream(Project project);

  /**
   * Returns a long-lived stream that emits the full set of records for a particular feature and
   * form on subscribe, and continues to return the full set each time a record is
   * added/changed/removed.
   */
  Flowable<ImmutableList<Record>> getRecordsOnceAndStream(Feature feature, String formId);

  /** Returns the feature with the specified UUID from the local data store, if found. */
  Maybe<Feature> getFeature(Project project, String featureId);

  /** Returns the record with the specified UUID from the local data store, if found. */
  Maybe<Record> getRecord(Feature feature, String recordId);

  /**
   * Returns all feature and record mutations in the local mutation queue relating to feature with
   * the specified id.
   */
  Single<ImmutableList<Mutation>> getPendingMutations(String featureId);

  /** Removes the provided feature and record mutations from the local mutation queue. */
  Completable removePendingMutations(ImmutableList<Mutation> mutations);

  /** Insert or replace feature in the local data store. */
  Completable mergeFeature(Feature feature);

  /** Applied pending local changes, then inserts or replaces the record in the local data store. */
  Completable mergeRecord(Record record);
}
