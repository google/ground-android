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

package com.google.android.gnd.model.observation;

import com.google.android.gnd.model.form.Field;
import com.google.auto.value.AutoValue;
import java8.util.Optional;

/** Represents a change to an individual form response in a observation. */
@AutoValue
public abstract class ResponseDelta {
  /** Returns the id of the form field being updated. */
  public abstract String getFieldId();

  public abstract Field.Type getFieldType();

  public abstract Optional<Response> getOriginalResponse();

  /** Returns the new value of the response, or empty if removed. */
  public abstract Optional<Response> getNewResponse();

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static Builder builder() {
    return new AutoValue_ResponseDelta.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setFieldId(String newFieldId);

    public abstract Builder setFieldType(Field.Type newFieldType);

    public abstract Builder setOriginalResponse(Optional<Response> newOriginalResponse);

    public abstract Builder setNewResponse(Optional<Response> newNewResponse);

    public abstract ResponseDelta build();
  }
}
