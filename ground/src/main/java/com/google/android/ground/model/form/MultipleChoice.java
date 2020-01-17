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

package com.google.android.ground.model.form;

import static com.google.android.ground.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import androidx.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java8.util.Optional;
import javax.annotation.Nullable;

/** A {@link Field} with pre-defined options for the user to choose from. */
@AutoValue
public abstract class MultipleChoice {
  public enum Cardinality {
    SELECT_ONE,
    SELECT_MULTIPLE
  }

  @NonNull
  public abstract ImmutableMap<String, Option> getOptions();

  @NonNull
  /** Returns the list of options sorted by index. */
  public ImmutableList<Option> getOptionsSorted() {
    return stream(getOptions().values().asList())
        .sorted((f1, f2) -> f1.getIndex() - f2.getIndex())
        .collect(toImmutableList());
  }

  @NonNull
  public Optional<Option> getOption(String id) {
    return Optional.ofNullable(getOptions().get(id));
  }

  @Nullable
  public abstract Cardinality getCardinality();

  public static Builder newBuilder() {
    return new AutoValue_MultipleChoice.Builder().setOptions(ImmutableMap.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setOptions(ImmutableMap<String, Option> newOptions);

    public abstract ImmutableMap.Builder<String, Option> optionsBuilder();

    public Builder putOption(String id, Option option) {
      optionsBuilder().put(id, option);
      return this;
    }

    public abstract Builder setCardinality(@Nullable Cardinality newCardinality);

    public abstract MultipleChoice build();
  }
}
