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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Represents a mutation of a record performed on the local device. Mutations are queued locally by
 * the UI, and are dequeued and sent to the remote data store by the background data sync service.
 */
@AutoValue
public abstract class RecordMutation extends Mutation {

  /** Returns the UUID of the record being modified. */
  public abstract String getRecordId();

  /** Returns the UUID of the form associated with this record. */
  public abstract String getFormId();

  /** Returns list of changes to responses included in this record mutation. */
  public abstract ImmutableList<ResponseDelta> getResponseDeltas();

  @Override
  public String toString() {
    return super.toString() + " deltas=" + getResponseDeltas();
  }

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static Builder builder() {
    return new AutoValue_RecordMutation.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder extends Mutation.Builder<Builder> {
    public abstract Builder setRecordId(String newRecordId);

    public abstract Builder setFormId(String newFormId);

    public abstract Builder setResponseDeltas(ImmutableList<ResponseDelta> newResponseDeltas);

    public abstract RecordMutation build();
  }
}
