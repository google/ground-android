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

package com.google.android.gnd.persistence.local.change;

import com.google.common.collect.ImmutableList;

/**
 * Represents the smallest local mutation that can be merged with data in the remote data store.
 *
 * @param <T> the type of entity being modified.
 */
public interface LocalChange {
  enum ChangeType {
    /** Indicates the entity should be created. */
    CREATE,

    /** Indicates an existing entity is to be updated. */
    UPDATE,

    /** Indicates an existing entity should be marked for deletion. */
    DELETE,
  }

  /** Returns the locally unique id of this change. */
  long getChangeId();

  /** Returns the type to which this change applies. */
  Class<?> getEntityType();

  /** Returns to globally unique id of the entity being modified. */
  String getEntityId();

  /** Returns the globally unique id of the user requesting the change. */
  String getUserId();

  /** Returns the list of individual changes applied to the entity. */
  ImmutableList<AttributeChange> getAttributeChanges();
}
