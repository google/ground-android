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

import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Record;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Provides access to local persistent data store. This is the app's canonical store for latest
 * state and queued local changes. Changes made locally are saved here before being synced with
 * remote stores, and remote updates are first persisted here before being made visible to the user.
 *
 * <p>Note: Implementations are expected to run each method defined by this interface as a single
 * transaction.
 */
public interface LocalDataStore {
  // Methods for writing data received from remote data store:

  /**
   * Stores an existing {@link Feature} received from the remote data store, reapplying any unsynced
   * local edits in the queue.
   *
   * @param feature the feature to be inserted or merged into the local db. remoteId must be
   *     non-null, localId is ignored.
   * @return once local writes are complete, emits the new state of the provided feature with its
   *     localId set and any pending local edits applied.
   */
  Single<Feature> insertOrMergeFeature(Feature feature);

  /**
   * Removes a {@link Feature} and all related {@link Record}s from the local db, deleting any
   * unsynced queued edits. Used when a feature is deleted from the remote repository.
   *
   * @param remoteId the remote id of the feature to be removed.
   * @return completes once local deletes have finished.
   */
  Completable removeFeature(String remoteId);

  /**
   * Stores an existing {@link Record} received from the remote data store, reapplying any unsynced
   * local edits in the queue.
   *
   * @param record the record to be inserted or merged into the local db. remoteId must be non-null,
   *     localId is ignored.
   * @return once local writes are complete, emits the new state of the provided record with its
   *     localId set and any pending local edits applied.
   */
  Single<Record> insertOrMergeRecord(Record record);

  /**
   * Removes a {@link Record} from the local db, deleting any unsynced queued edits. Used when a
   * record is deleted from the remote repository.
   *
   * @param remoteId the remote id of the record to be removed.
   * @return completes once local deletes have finished.
   */
  Completable removeRecord(String remoteId);

  // Methods for saving changes made locally:

  /**
   * Adds a new {@link Feature} to the local db, queueing it to be written to the remote data store.
   *
   * @param feature the new feature to be created. Its localId and remoteId fields are ignored.
   * @return once local writes are complete, emits the new feature with a generated localId set.
   */
  Single<Feature> createNewFeature(Feature feature);

  /**
   * Applies edits to a {@link Feature} in the local db, queueing edits to be synced to the remote
   * data store.
   *
   * @param id the local id of the feature to be updated.
   * @param edits the feature attributes to be updated locally and remotely.
   * @return once local writes are complete, emits the new state of {@link Feature} in the local db
   *     after applying provided edits.
   */
  // TODO: Create FeatureEdit to represent location change and eventually edits to shape, caption,
  // etc. and delete FeatureUpdate.
  Single<Feature> applyFeatureEdits(int id, User user, ImmutableList<FeatureEdit> edits);

  /**
   * Marks a {@link Feature} as deleted locally, queuing it to be deleted remotely as well.
   * repository.
   *
   * @param localId the local id of the feature to be deleted.
   * @return completes once local deletes have finished.
   */
  Completable deleteFeature(int localId);

  /**
   * Adds a new {@link Record} to the local db, queueing it to be written to the remote data store.
   *
   * @param record the new record to be created. Its localId and remoteId fields are ignored.
   * @return once local writes are complete, emits the new record with a generated localId set.
   */
  Single<Record> createNewRecord(Record record);

  /**
   * Applies edits to a {@link Record} in the local db, queueing edits to be synced to the remote
   * data store.
   *
   * @param id the local id of the record to be updated.
   * @param edits the form responses to be updated locally and remotely.
   * @return once local writes are complete, emits the new state of {@link Record} in the local db
   *     after applying provided edits.
   */
  // TODO: Rescope ResponseUpdate and rename to RecordEdit.
  Single<Record> applyRecordEdits(ImmutableList<RecordEdit> edits, User user);

  /**
   * Marks a {@link Record} as deleted locally, queuing it to be deleted remotely as well.
   *
   * @param localId the local id of the record to be deleted.
   * @return completes once local deletes have finished.
   */
  Completable deleteRecord(int localId);
}
