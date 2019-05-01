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
import com.google.common.collect.ImmutableList;

/**
 * Represents a mutation that can be applied to local data and queued for sync with the remote data
 * store.
 */
@AutoValue
public abstract class LocalChange {
  public enum ChangeType {
    /** Indicates a new feature should be created. */
    CREATE_FEATURE,

    /** Indicates an existing feature should be updated. */
    UPDATE_FEATURE,

    /** Indicates an existing feature should be marked for deletion. */
    DELETE_FEATURE,

    /** Indicates a new record should be created. */
    CREATE_RECORD,

    /** Indicates an existing record should be updated. */
    UPDATE_RECORD,

    /** Indicates an existing record should be marked for deletion. */
    DELETE_RECORD
  }

  /** Returns the locally unique id of this change. */
  public abstract long getChangeId();

  /**
   * Returns the type of change (i.e., create, update, delete) and the type of entity this change
   * represents.
   */
  public abstract ChangeType getChangeType();

  /** Returns the unique id of the project in which this feature resides. */
  public abstract String getProjectId();

  /** Returns the globally unique id of the entity being modified. */
  public abstract String getEntityId();

  /** Returns the globally unique id of the user requesting the change. */
  public abstract String getUserId();

  /** Returns the list of individual changes applied to the entity. */
  public abstract ImmutableList<AttributeChange> getAttributeChanges();

  public static Builder builder() {
    return new AutoValue_LocalChange.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setChangeId(long newChangeId);

    public abstract Builder setChangeType(ChangeType newChangeType);

    public abstract Builder setProjectId(String newProjectId);

    public abstract Builder setEntityId(String newEntityId);

    public abstract Builder setUserId(String newUserId);

    public abstract Builder setAttributeChanges(ImmutableList<AttributeChange> newAttributeChanges);

    public abstract LocalChange build();
  }
}
