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

package com.google.android.gnd.vo;

import static java8.util.stream.StreamSupport.stream;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import javax.annotation.Nullable;

/** A {@link Field} with pre-defined options for the user to choose from. */
@AutoValue
public abstract class MultipleChoice {
  public enum Cardinality {
    SELECT_ONE,
    SELECT_MULTIPLE
  }

  public abstract ImmutableList<Option> getOptions();

  public Optional<Option> getOption(String code) {
    return stream(getOptions()).filter(o -> o.getCode().equals(code)).findFirst();
  }

  @Nullable
  public abstract Cardinality getCardinality();

  public Optional<Integer> getIndex(String code) {
    for (int i = 0; i < getOptions().size(); i++) {
      if (getOptions().get(i).getCode().equals(code)) {
        return Optional.of(i);
      }
    }
    return Optional.empty();
  }

  public static Builder newBuilder() {
    return new AutoValue_Form_MultipleChoice.Builder().setOptions(ImmutableList.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setOptions(ImmutableList<Option> newOptions);

    public abstract ImmutableList.Builder<Option> optionsBuilder();

    public abstract Builder setCardinality(@Nullable Cardinality newCardinality);

    public abstract MultipleChoice build();
  }
}
