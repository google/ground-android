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

import static java8.util.stream.StreamSupport.stream;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Option;
import java.util.List;
import java8.util.Optional;
import java8.util.stream.Collectors;

/** User responses to a select-one (radio) or select-multiple (checkbox) field. */
public class MultipleChoiceResponse implements Response {

  private List<String> choices;

  public MultipleChoiceResponse(List<String> choices) {
    this.choices = choices;
  }

  public List<String> getChoices() {
    return choices;
  }

  public Optional<String> getFirstId() {
    return stream(choices).findFirst();
  }

  public boolean isSelected(@NonNull Option option) {
    return choices.contains(option.getId());
  }

  public String getSummaryText(@NonNull Field field) {
    return getDetailsText(field);
  }

  // TODO: Make these inner classes non-static and access Form directly.
  public String getDetailsText(@NonNull Field field) {
    return stream(choices)
        .map(field.getMultipleChoice()::getOptionById)
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
    if (obj instanceof MultipleChoiceResponse) {
      return choices.equals(((MultipleChoiceResponse) obj).choices);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return choices.hashCode();
  }

  @NonNull
  @Override
  public String toString() {
    return stream(choices).sorted().collect(Collectors.joining(","));
  }

  public static Optional<Response> fromList(@NonNull List<String> codes) {
    if (codes.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(new MultipleChoiceResponse(codes));
    }
  }
}
