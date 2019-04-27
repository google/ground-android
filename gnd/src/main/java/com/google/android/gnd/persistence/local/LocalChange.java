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

import java8.util.Optional;

/**
 * Represents the smallest unit of change to an entity made locally that can be merged with changes
 * received from the remote data store.
 *
 * @param <T> the class of a builder for the type under mutation.
 * @param <V> the class of the value being set or cleared.
 */
public interface LocalChange<T, V> {
  /** Returns the builder class to which this change applies. */
  Class<T> getEntityBuilderType();

  /** Returns the value class to which this change applies. */
  Class<V> getValueType();

  /** Returns the locally unique id of this change. */
  long getChangeId();

  /** Returns to globally unique id of the entity being modified. */
  String getEntityId();

  /**
   * Returns the value before this change is applied, if present. If the value was not specified
   * (missing) empty is returned.
   */
  Optional<V> getOldValue();

  /**
   * Returns the value to be set after this change is applied. If empty, the change indicates the
   * current value should be cleared.
   */
  Optional<V> getNewValue();

  /** Applies the change to the provided builder, returning a reference to the same for chaining. */
  T apply(T entityBuilder);

  /**
   * Rolls back the change on the provided builder, returning a reference to the same for chaining.
   */
  T rollBack(T entityBuilder);
}
