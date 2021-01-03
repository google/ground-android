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

package com.google.android.gnd.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Date;

/**
 * Represents a mutation that can be applied to local data and queued for sync with the remote data
 * store.
 */
public abstract class Mutation<B extends Mutation.Builder> {

  public enum Type {
    /** Indicates a new entity should be created. */
    CREATE,

    /** Indicates an existing entity should be updated. */
    UPDATE,

    /** Indicates an existing entity should be marked for deletion. */
    DELETE,

    /** Indicates database skew or an implementation bug. */
    UNKNOWN
  }

  /** Returns the locally unique id of this change. */
  @Nullable
  public abstract Long getId();

  /**
   * Returns the type of change (i.e., create, update, delete) and the type of entity this change
   * represents.
   */
  public abstract Type getType();

  /** Returns the unique id of the project in which this feature resides. */
  public abstract String getProjectId();

  /** Returns the UUID of the feature to which the mutated entity belongs. */
  public abstract String getFeatureId();

  /** Returns the UUID of the layer being modified. */
  public abstract String getLayerId();

  /** Returns the id of the user requesting the change. */
  public abstract String getUserId();

  /** Returns the time the mutation was requested on the client. */
  @NonNull
  public abstract Date getClientTimestamp();

  public abstract long getRetryCount();

  @Nullable
  public abstract String getLastError();

  @Override
  public String toString() {
    return getClass().getSimpleName() + " type=" + getType() + " id=" + getId();
  }

  public abstract B toBuilder();

  public abstract static class Builder<T extends Builder> {

    public abstract T setId(@Nullable Long newId);

    public abstract T setFeatureId(String newFeatureId);

    public abstract T setLayerId(String newLayerId);

    public abstract T setType(Type newType);

    public abstract T setProjectId(String newProjectId);

    public abstract T setUserId(String newUserId);

    public abstract T setClientTimestamp(@NonNull Date newClientTimestamp);

    public abstract T setRetryCount(long newRetryCount);

    public abstract T setLastError(@Nullable String lastError);

    public abstract Mutation build();
  }
}
