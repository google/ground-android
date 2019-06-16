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

import com.google.android.gnd.vo.Record.Response;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java8.util.Optional;

/** Represents mutation of a record in the local to be queued for sync with remote store. */
@AutoValue
public abstract class RecordMutation extends Mutation {

  /** Returns the UUID of the record being modified. */
  public abstract String getRecordId();

  /** Returns the UUID of the feature to which the mutated record belongs. */
  public abstract String getFeatureId();

  /** Returns the UUID of the form associated with this record. */
  public abstract String getFormId();

  /**
   * Returns a map keyed by response element id. The presence of a map entry indicates a specific
   * response was modified. If the value is empty, it indicates the response was removed/cleared.
   */
  public abstract ImmutableMap<String, Optional<Response>> getModifiedResponses();

  public static Builder builder() {
    return new AutoValue_RecordMutation.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder extends Mutation.Builder<Builder> {
    public abstract Builder setRecordId(String newRecordId);

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setFormId(String newFormId);

    public abstract Builder setModifiedResponses(
        ImmutableMap<String, Optional<Response>> newModifiedResponses);

    public abstract RecordMutation build();
  }
}
