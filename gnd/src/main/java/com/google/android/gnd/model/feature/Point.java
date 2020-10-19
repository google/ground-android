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

package com.google.android.gnd.model.feature;

import androidx.annotation.NonNull;
import com.google.auto.value.AutoValue;

/** The location of a single point on the map. */
@AutoValue
public abstract class Point {
  public abstract double getLatitude();

  public abstract double getLongitude();

  @NonNull
  public static Builder newBuilder() {
    return new AutoValue_Point.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setLatitude(double newLatitude);

    public abstract Builder setLongitude(double newLongitude);

    @NonNull
    public abstract Point build();
  }
}
