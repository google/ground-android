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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java8.util.Optional;

import static java8.util.stream.StreamSupport.stream;

/** Metadata and schema for a single Ground project. */
@AutoValue
public abstract class Project {
  public abstract String getId();

  public abstract String getTitle();

  public abstract String getDescription();

  public abstract ImmutableList<PlaceType> getPlaceTypes();

  public Optional<PlaceType> getPlaceType(String placeTypeId) {
    return stream(getPlaceTypes()).filter(p -> p.getId().equals(placeTypeId)).findFirst();
  }

  public static Builder newBuilder() {
    return new AutoValue_Project.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setTitle(String newTitle);

    public abstract Builder setDescription(String newDescription);

    public abstract Builder setPlaceTypes(ImmutableList<PlaceType> newPlaceTypes);

    public abstract Project build();
  }
}
