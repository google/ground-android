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

package com.google.android.gnd.repository.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/**
 * Represents a lat/lng coordinate in FeatureEdit instances.
 */
@AutoValue
public abstract class Coordinates {
  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "latitude")
  public abstract double getLatitude();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "longitude")
  public abstract double getLongitude();

  // Auto-generated boilerplate:

  public static Coordinates create(double latitude, double longitude) {
    return builder()
        .setLatitude(latitude)
        .setLongitude(longitude)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_Coordinates.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setLatitude(double newLatitude);

    public abstract Builder setLongitude(double newLongitude);

    public abstract Coordinates build();
  }
}
