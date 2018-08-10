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
import com.google.android.gnd.vo.Form.Field;
import com.google.android.gnd.vo.Form.MultipleChoice.Option;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.List;
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

  public interface Value {

    String getSummaryText(Field field);

    String getDetailsText(Field field);

    static String toString(Optional<Value> value) {
      return value.map(Value::toString).orElse("");
    }
  }

  public static class TextValue implements Value {

    private String text;

    public TextValue(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }

    @Override
    public String getSummaryText(Field field) {
      return text;
    }

    @Override
    public String getDetailsText(Field field) {
      return text;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof TextValue)) {
        return false;
      }
      return text.equals(((TextValue) obj).text);
    }

    @Override
    public int hashCode() {
      return text.hashCode();
    }

    @Override
    public String toString() {
      return text;
    }

    public static Optional<Value> fromString(String text) {
      text = text.trim();
      return text.isEmpty() ? Optional.empty() : Optional.of(new TextValue(text));
    }
  }

  public static class MultipleChoiceValue implements Value {

    private List<String> choices;

    public MultipleChoiceValue(List<String> choices) {
      this.choices = choices;
    }

    public List<String> getChoices() {
      return choices;
    }

    public Optional<String> getFirstCode() {
      return stream(choices).findFirst();
    }

    public boolean isSelected(Option option) {
      return choices.contains(option.getCode());
    }

    public String getSummaryText(Field field) {
      return toString();
    }

    // TODO: Make these inner classes non-static and access Form directly.
    public String getDetailsText(Field field) {
      return stream(choices)
          .map(v -> field.getMultipleChoice().getOption(v))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(Option::getLabel)
          .sorted()
          .collect(Collectors.joining(", "));
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof MultipleChoiceValue)) {
        return false;
      }
      return choices.equals(((MultipleChoiceValue) obj).choices);
    }

    @Override
    public int hashCode() {
      return choices.hashCode();
    }

    @Override
    public String toString() {
      return stream(choices).sorted().collect(Collectors.joining(","));
    }

    public static Optional<Value> fromList(List<String> codes) {
      if (codes.isEmpty()) {
        return Optional.empty();
      } else {
        return Optional.of(new MultipleChoiceValue(codes));
      }
    }
  }
}
