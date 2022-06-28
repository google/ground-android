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

package com.google.android.gnd.model.task;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.submission.Response;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java8.util.Comparators;
import java8.util.Optional;

/**
 * Describes the layout, field types, and validation rules of a user-defined task. Does not contain
 * actual task responses (see {@link Response} instead.
 */
@AutoValue
public abstract class Task {

  public abstract String getId();

  public abstract ImmutableList<Step> getSteps();

  public ImmutableList<Step> getStepsSorted() {
    return stream(getSteps())
        .sorted(Comparators.comparing(e -> e.getIndex()))
        .collect(toImmutableList());
  }

  public Optional<Field> getField(String id) {
    return stream(getSteps())
        .map(Step::getField)
        .filter(f -> f != null && f.getId().equals(id))
        .findFirst();
  }

  public static Builder newBuilder() {
    return new AutoValue_Task.Builder().setSteps(ImmutableList.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setSteps(ImmutableList<Step> newStepsList);

    public abstract Task build();
  }
}
