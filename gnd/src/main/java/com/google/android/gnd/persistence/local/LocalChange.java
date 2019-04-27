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

/**
 * Represents the smallest unit of change to an entity made locally that can be merged with changes
 * received from the remote data store.
 *
 * @param <T> the class of a builder for the type under mutation.
 * @param <V> the class of the value being set or cleared.
 */
public interface LocalChange<T> {
  // NOTE: It's not clear if EntityType and EntityId fields are needed here. We'll add them to
  // the right place while implementing.

  /** Returns the type to which this change applies. */
  Class<T> getEntityType();

  /** Returns the locally unique id of this change. */
  long getChangeId();

  /** Returns to globally unique id of the entity being modified. */
  String getEntityId();
}
