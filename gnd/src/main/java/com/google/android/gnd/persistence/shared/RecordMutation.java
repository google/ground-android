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
import java.util.Map;
import java8.util.Optional;

/** Represents mutation of a record in the local to be queued for sync with remote store. */
public abstract @AutoValue class RecordMutation extends Mutation {

  /**
   * Returns a map keyed by response element id. The presence of a map entry indicates a specific
   * response was modified. If the value is empty, it indicates the response was removed/cleared.
   */
  public abstract Map<String, Optional<Response>> getNewResponses();

  public static Builder builder() {
    return new AutoValue_RecordMutation.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder extends Mutation.Builder<Builder> {

    public abstract Builder setNewResponses(Map<String, Optional<Response>> newNewResponses);

    public abstract RecordMutation build();
  }
}
