/*
 * Copyright 2021 Google LLC
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

package com.google.android.gnd.model.feature;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;

/** User-defined map feature consisting of a set of geometries defined in GeoJson format. */
@AutoValue
public abstract class GeoJsonFeature extends Feature<GeoJsonFeature.Builder> {
  // TODO: Use builder() or newBuilder() consistently.
  public static Builder newBuilder() {
    return new AutoValue_GeoJsonFeature.Builder();
  }

  public abstract String getGeoJsonString();

  @Memoized
  @Override
  public abstract int hashCode();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder extends Feature.Builder<Builder> {

    public abstract Builder setGeoJsonString(String newGeoJsonString);

    public abstract GeoJsonFeature build();
  }
}
