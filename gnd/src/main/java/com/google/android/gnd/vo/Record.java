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

import static java8.util.stream.StreamSupport.stream;

import android.support.annotation.Nullable;
import com.google.android.gnd.vo.Form.MultipleChoice.Option;
import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java8.util.Optional;
import java8.util.stream.Collectors;

@AutoValue
public abstract class Record {
  @Nullable
  public abstract String getId();

  public abstract Project getProject();

  public abstract Place getPlace();

  public abstract Form getForm();

  @Nullable
  public abstract Timestamps getServerTimestamps();

  @Nullable
  public abstract Timestamps getClientTimestamps();

  // TODO: Make Immutable and/or make private and expose custom accessors.
  public abstract ImmutableMap<String, Value> getValueMap();

  public static Builder newBuilder() {
    return new AutoValue_Record.Builder();
  }

  public Optional<Value> getValue(String id) {
    return Optional.ofNullable(getValueMap().get(id));
  }

  public abstract Record.Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(@Nullable String newId);

    public abstract Builder setProject(Project project);

    public abstract Builder setPlace(Place place);

    public abstract Builder setForm(Form form);

    public abstract Builder setServerTimestamps(@Nullable Timestamps newServerTimestamps);

    public abstract Builder setClientTimestamps(@Nullable Timestamps newClientTimestamps);

    public abstract ImmutableMap.Builder<String, Value> valueMapBuilder();

    public Builder putValue(String id, Value value) {
      valueMapBuilder().put(id, value);
      return this;
    }

    public Builder putAllValues(Map<String, Value> values) {
      valueMapBuilder().putAll(values);
      return this;
    }

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

    public Optional<String> getFirstCode() {
      if (!getType().equals(Type.CHOICES)) {
        return Optional.empty();
      }
      return stream(getChoices().getCodes()).findFirst();
    }

    public boolean isSelected(Option option) {
      return getChoices().getCodes().contains(option.getCode());
    }

    public String getSummaryText() {
      switch (getType()) {
        case TEXT:
          return getText();
        case NUMBER:
          // TODO: int vs float? Format correctly.
          return Float.toString(getNumber());
        case CHOICES:
          // TODO: i18n of separator.
          return stream(getChoices().getCodes()).sorted().collect(Collectors.joining(","));
        default:
          return "";
      }
    }

    // TODO: Make these inner classes non-static and access Form directly.
    public String getDetailsText(Form.Field field) {
      switch (getType()) {
        case TEXT:
          return getText();
        case NUMBER:
          // TODO: int vs float? Format correctly.
          return Float.toString(getNumber());
        case CHOICES:
          // TODO: i18n of separator.
          return stream(getChoices().getCodes())
            .map(v -> field.getMultipleChoice().getOption(v))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Option::getLabel)
            .sorted()
            .collect(Collectors.joining(", "));
        default:
          return "";
      }
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
