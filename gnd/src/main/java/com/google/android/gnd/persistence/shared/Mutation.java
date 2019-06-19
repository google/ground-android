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

package com.google.android.gnd.persistence.shared;

import androidx.annotation.Nullable;

/**
 * Represents a mutation that can be applied to local data and queued for sync with the remote data
 * store.
 */
public abstract class Mutation {
  public enum Type {
    /** Indicates a new entity should be created. */
    CREATE,

    /** Indicates an existing entity should be updated. */
    UPDATE,

    /** Indicates an existing entity should be marked for deletion. */
    DELETE;
  }

  /** Returns the locally unique id of this change. */
  @Nullable
  public abstract Long getChangeId();

  /**
   * Returns the type of change (i.e., create, update, delete) and the type of entity this change
   * represents.
   */
  public abstract Type getType();

  /** Returns the unique id of the project in which this feature resides. */
  public abstract String getProjectId();

  /** Returns the globally unique id of the user requesting the change. */
  // TODO(#101): Make NonNull.
  @Nullable
  public abstract String getUserId();

  public abstract static class Builder<T extends Builder> {

    public abstract T setChangeId(@Nullable Long newChangeId);

    public abstract T setType(Type newType);

    public abstract T setProjectId(String newProjectId);

    public abstract T setUserId(String newUserId);
  }
}
