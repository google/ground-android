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

import com.google.auto.value.AutoValue;

/**
 * Represents the creation of a new entity in the local data store that needs to be also created in
 * the remote data store. Even add and update with a locally generated id may be equivalent on some
 * remote data stores (i.e., "set" operation), the "create" operation must be queued separately to
 * allow rollback on sync failure.
 *
 * @param <T> the type of entity being created.
 */
@AutoValue
public abstract class CreateEntityChange<T> implements LocalChange<T> {

  public static <T> Builder<T> builder() {
    return new AutoValue_CreateEntityChange.Builder<>();
  }

  @AutoValue.Builder
  public abstract static class Builder<T> {
    public abstract Builder<T> setEntityType(Class<T> newEntityType);

    public abstract Builder<T> setChangeId(long newChangeId);

    public abstract Builder<T> setEntityId(String newEntityId);

    public abstract Builder<T> setUserId(String newUserId);

    public abstract CreateEntityChange<T> build();
  }
}
