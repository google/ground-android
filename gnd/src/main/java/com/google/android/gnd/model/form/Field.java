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

package com.google.android.gnd.model.form;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** A single field on a form for which the user may provide data. */
@AutoValue
public abstract class Field {
  public enum Type {
    TEXT,
    MULTIPLE_CHOICE
  }

  @Nullable
  public abstract String getId();

  @Nullable
  public abstract Type getType();

  @Nullable
  public abstract String getLabel();

  public abstract boolean isRequired();

  @Nullable
  public abstract MultipleChoice getMultipleChoice();

  public static Builder newBuilder() {
    return new AutoValue_Field.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(@Nullable String newId);

    public abstract Builder setType(@Nullable Type newType);

    public abstract Builder setLabel(@Nullable String newLabel);

    public abstract Builder setRequired(boolean newRequired);

    public abstract Builder setMultipleChoice(@Nullable MultipleChoice multipleChoice);

    public abstract Field build();
  }
}
