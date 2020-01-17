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

package com.google.android.ground.model.form;

import static java8.util.stream.StreamSupport.stream;

import com.google.android.ground.model.observation.Response;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import javax.annotation.Nullable;

/**
 * Describes the layout, field types, and validation rules of a user-defined form. Does not contain
 * actual form responses (see {@link Response} instead.
 */
@AutoValue
public abstract class Form {
  @Nullable
  public abstract String getId();

  public abstract ImmutableList<Element> getElements();

  public Optional<Field> getField(String id) {
    return stream(getElements())
        .map(Element::getField)
        .filter(f -> f != null && f.getId().equals(id))
        .findFirst();
  }

  public static Builder newBuilder() {
    return new AutoValue_Form.Builder().setElements(ImmutableList.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(@Nullable String newId);

    public abstract Builder setElements(ImmutableList<Element> newElementsList);

    public abstract Form build();
  }
}
