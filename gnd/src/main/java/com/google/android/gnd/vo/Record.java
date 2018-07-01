/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.vo;

import com.google.android.gnd.repository.RecordSummary;
import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Map;

@AutoValue
public abstract class Record {
  public abstract String getId();

  public abstract String getPlaceTypeId();

  public abstract String getFormId();

  public abstract Timestamps getServerTimestamps();

  public abstract Timestamps getClientTimestamps();

  // TODO: Make Immutable and/or make private and expose custom accessors.
  public abstract Map<String, Value> getValueMap();

  public static Builder newBuilder() {
    return new AutoValue_Record.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setPlaceTypeId(String newPlaceTypeId);

    public abstract Builder setFormId(String newFormId);

    public abstract Builder setServerTimestamps(Timestamps newServerTimestamps);

    public abstract Builder setClientTimestamps(Timestamps newClientTimestamps);

    public abstract Builder setValueMap(Map<String, Value> newValue);

    public abstract Record build();
  }

  @AutoOneOf(Value.Type.class)
  public abstract static class Value {
    public enum Type {
      TEXT,
      NUMBER,
      CHOICES
    }

    public abstract Type getType();

    public abstract String getText();

    public abstract float getNumber();

    public abstract Choices getChoices();

    public static Value ofText(String text) {
      return AutoOneOf_Record_Value.text(text);
    }

    public static Value ofNumber(float number) {
      return AutoOneOf_Record_Value.number(number);
    }

    public static Value ofChoices(Choices choices) {
      return AutoOneOf_Record_Value.choices(choices);
    }

    @Override
    public String toString() {
      return RecordSummary.toSummaryText(this);
    }
  }

  @AutoValue
  public abstract static class Choices {
    public abstract ImmutableList<String> getCodes();

    public static Builder newBuilder() {
      return new AutoValue_Record_Choices.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setCodes(ImmutableList<String> newCodes);

      public abstract ImmutableList.Builder<String> codesBuilder();

      public abstract Choices build();
    }
  }
}
