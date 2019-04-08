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

import androidx.annotation.Nullable;
import com.google.android.gnd.system.AuthenticationManager.User;
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

  @Nullable
  public abstract Project getProject();

  @Nullable
  public abstract Feature getFeature();

  @Nullable
  public abstract Form getForm();

  @Nullable
  public abstract User getCreatedBy();

  @Nullable
  public abstract User getModifiedBy();

  @Nullable
  public abstract Timestamps getServerTimestamps();

  @Nullable
  public abstract Timestamps getClientTimestamps();

  public abstract ImmutableMap<String, Response> getResponseMap();

  public static Builder newBuilder() {
    return new AutoValue_Record.Builder();
  }

  public Optional<Response> getResponse(String id) {
    return Optional.ofNullable(getResponseMap().get(id));
  }

  public abstract Record.Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(@Nullable String newId);

    public abstract Builder setProject(@Nullable Project project);

    public abstract Builder setFeature(@Nullable Feature feature);

    public abstract Builder setForm(@Nullable Form form);

    public abstract Builder setCreatedBy(@Nullable User user);

    public abstract Builder setModifiedBy(@Nullable User user);

    public abstract Builder setServerTimestamps(@Nullable Timestamps newServerTimestamps);

    public abstract Builder setClientTimestamps(@Nullable Timestamps newClientTimestamps);

    public abstract ImmutableMap.Builder<String, Response> responseMapBuilder();

    public Builder putResponse(String id, Response response) {
      responseMapBuilder().put(id, response);
      return this;
    }

    public Builder putAllResponses(Map<String, Response> responses) {
      responseMapBuilder().putAll(responses);
      return this;
    }

    public abstract Record build();
  }

  public interface Response {

    String getSummaryText(Field field);

    String getDetailsText(Field field);

    static String toString(Optional<Response> response) {
      return response.map(Response::toString).orElse("");
    }

    boolean isEmpty();
  }

  public static class TextResponse implements Response {

    private String text;

    public TextResponse(String text) {
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
    public boolean isEmpty() {
      return text.trim().isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof TextResponse)) {
        return false;
      }
      return text.equals(((TextResponse) obj).text);
    }

    @Override
    public int hashCode() {
      return text.hashCode();
    }

    @Override
    public String toString() {
      return text;
    }

    public static Optional<Response> fromString(String text) {
      return text.isEmpty() ? Optional.empty() : Optional.of(new TextResponse(text));
    }
  }

  public static class MultipleChoiceResponse implements Response {

    private List<String> choices;

    public MultipleChoiceResponse(List<String> choices) {
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
          .map(code -> field.getMultipleChoice().getOption(code))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(Option::getLabel)
          .sorted()
          .collect(Collectors.joining(", "));
    }

    @Override
    public boolean isEmpty() {
      return choices.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof MultipleChoiceResponse)) {
        return false;
      }
      return choices.equals(((MultipleChoiceResponse) obj).choices);
    }

    @Override
    public int hashCode() {
      return choices.hashCode();
    }

    @Override
    public String toString() {
      return stream(choices).sorted().collect(Collectors.joining(","));
    }

    public static Optional<Response> fromList(List<String> codes) {
      if (codes.isEmpty()) {
        return Optional.empty();
      } else {
        return Optional.of(new MultipleChoiceResponse(codes));
      }
    }
  }
}
