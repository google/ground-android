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

import com.google.android.gnd.persistence.local.change.LocalChange;
import com.google.android.gnd.persistence.remote.RemoteChange;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Provides access to local persistent data store, the canonical store for latest state. Local
 * changes are saved here before being written to the remote data store, and both local and remote
 * updates are always persisted here before being made visible to the user.
 */
public interface LocalDataStore {
  /**
   * Applies the provided change to existing local data store and adds it to the queue of pending
   * changes. Returns the new id of the {@link LocalChange} in the queue. The {@code id} in the
   * provided {@link LocalChange} is ignored.
   */
  Single<Long> applyAndEnqueue(LocalChange localChange);

  /**
   * Returns all pending changes in the queue that apply to the feature with the specified id, or an
   * empty list if none are present. Changes marked "failed" are not returned.
   */
  Single<ImmutableList<LocalChange>> getPendingChanges(String featureId);

  /**
   * Removes the specified changes from the queue, called once the changes have successfully been
   * written to the remote store.
   */
  Completable dequeue(ImmutableList<LocalChange> localChanges);

  /** Flags the specified changes as failed, allowing the user to later retry or abandon them. */
  Completable markFailed(ImmutableList<LocalChange> localChanges);

  // TODO: Define methods to allow retry or rollback of failed changes.

  /**
   * Merges remote changes with pending local edits by overwriting the local data store with the
   * remote change, and reapplying any unsynced pending local changes.
   */
  Completable merge(RemoteChange<?> remoteChange);
}
