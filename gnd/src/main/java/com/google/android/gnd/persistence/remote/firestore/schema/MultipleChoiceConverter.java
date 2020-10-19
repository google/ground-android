/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.persistence.remote.firestore.schema;

import static com.google.android.gnd.util.Enums.toEnum;
import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.form.MultipleChoice;
import java8.util.Comparators;

class MultipleChoiceConverter {

  @NonNull
  static MultipleChoice toMultipleChoice(@NonNull ElementNestedObject em) {
    MultipleChoice.Builder mc = MultipleChoice.newBuilder();
    mc.setCardinality(toEnum(MultipleChoice.Cardinality.class, em.getCardinality()));
    if (em.getOptions() != null) {
      mc.setOptions(
          stream(em.getOptions().entrySet())
              .sorted(Comparators.comparing(e -> e.getValue().getIndex()))
              .map(e -> OptionConverter.toOption(e.getKey(), e.getValue()))
              .collect(toImmutableList()));
    }
    return mc.build();
  }
}
