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

package com.google.android.gnd.persistence.remote;

import java8.util.Optional;

public interface RemoteChange<T> {
  enum ChangeType {
    /**
     * Indicates the entity was loaded from remote data store. The occurs after listening to remote
     * changes, or when when an entity is added to the remote data store.
     */
    LOADED,

    /**
     * Indicates the entity was modified in the remote data store. This includes changes in
     * "deletion status" as well as other edits.
     */
    MODIFIED,

    /**
     * Indicated the entity was removed from the remote data store. Note that soft deletion (i.e.
     * change in "deletion status") triggers {@link ChangeType#MODIFIED}.
     */
    REMOVED,
  }

  /** Returns the type of remote change this represents. */
  ChangeType getChangeType();

  /** Returns the class of the entity that was modified remotely. */
  Class<T> getEntityType();

  /** Returns the unique id of the entity that was modified remotely. */
  String getId();

  /**
   * For changes of type {@link ChangeType#LOADED} and {@link ChangeType#MODIFIED}, returns the new
   * state of the changed entity. This will always be empty for {@link ChangeType#REMOVED}.
   */
  Optional<T> getEntity();
}
