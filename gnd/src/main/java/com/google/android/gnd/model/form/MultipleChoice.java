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

import static java8.util.stream.StreamSupport.stream;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;

/** A {@link Field} with pre-defined options for the user to choose from. */
@AutoValue
public abstract class MultipleChoice {
  public enum Cardinality {
    SELECT_ONE,
    SELECT_MULTIPLE
  }

  public abstract ImmutableList<Option> getOptions();

  public Optional<Option> getOptionById(String id) {
    return stream(getOptions()).filter(o -> o.getId().equals(id)).findFirst();
  }

  public abstract Cardinality getCardinality();

  public Optional<Integer> getIndex(String id) {
    for (int i = 0; i < getOptions().size(); i++) {
      if (getOptions().get(i).getId().equals(id)) {
        return Optional.of(i);
      }
    }
    return Optional.empty();
  }

  public static Builder newBuilder() {
    return new AutoValue_MultipleChoice.Builder().setOptions(ImmutableList.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setOptions(ImmutableList<Option> newOptions);

    public abstract Builder setCardinality(Cardinality newCardinality);

    public abstract MultipleChoice build();
  }
}
