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

package com.google.android.gnd.model.submission;

import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.task.MultipleChoice;
import com.google.android.gnd.model.task.Option;
import java.util.List;
import java8.util.Optional;
import java8.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * User responses to a select-one (radio) or select-multiple (checkbox) field.
 */
public class MultipleChoiceResponse implements Response {

  private static final long serialVersionUID = 1L;

  private final MultipleChoice multipleChoice;
  private final List<String> selectedOptionIds;

  public MultipleChoiceResponse(MultipleChoice multipleChoice, List<String> selectedOptionIds) {
    this.multipleChoice = multipleChoice;
    this.selectedOptionIds = selectedOptionIds;
  }

  public static Optional<Response> fromList(MultipleChoice multipleChoice, List<String> codes) {
    if (codes.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(new MultipleChoiceResponse(multipleChoice, codes));
    }
  }

  public MultipleChoice getMultipleChoice() {
    return multipleChoice;
  }

  public List<String> getSelectedOptionIds() {
    return selectedOptionIds;
  }

  public Optional<String> getFirstId() {
    return stream(selectedOptionIds).findFirst();
  }

  public boolean isSelected(Option option) {
    return selectedOptionIds.contains(option.getId());
  }

  public String getSummaryText() {
    return getDetailsText();
  }

  // TODO: Make these inner classes non-static and access Task directly.
  public String getDetailsText() {
    return stream(selectedOptionIds)
        .map(getMultipleChoice()::getOptionById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(Option::getLabel)
        .sorted()
        .collect(Collectors.joining(", "));
  }

  @Override
  public boolean isEmpty() {
    return selectedOptionIds.isEmpty();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MultipleChoiceResponse) {
      return selectedOptionIds.equals(((MultipleChoiceResponse) obj).selectedOptionIds);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return selectedOptionIds.hashCode();
  }

  @NotNull
  @Override
  public String toString() {
    return stream(selectedOptionIds).sorted().collect(Collectors.joining(","));
  }
}
