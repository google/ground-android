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

package com.google.android.gnd.model.observation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Field;
import java8.util.Objects;

/** A user provided response to a text {@link Field}. */
public class LocationResponse implements Response {

  @Nullable private Point coordinates;

  private double altitude;

  private float accuracy;

  public LocationResponse(@Nullable Point coordinates, double altitude, float accuracy) {
    this.coordinates = coordinates;
    this.altitude = altitude;
    this.accuracy = accuracy;
  }

  @Nullable
  public Point getCoordinates() {
    return coordinates;
  }

  public double getAltitude() {
    return altitude;
  }

  public float getAccuracy() {
    return accuracy;
  }

  @Override
  public String getSummaryText(Field field) {
    return toString();
  }

  @Override
  public String getDetailsText(Field field) {
    if (coordinates == null) {
      return "";
    }
    // TODO: i18n.
    return getSummaryText(field) + ", " + getAltitude() + " @ " + accuracy + "m";
  }

  @Override
  public boolean isEmpty() {
    return coordinates == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocationResponse that = (LocationResponse) o;
    return Double.compare(that.altitude, altitude) == 0
        && Float.compare(that.accuracy, accuracy) == 0
        && Objects.equals(coordinates, that.coordinates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinates, altitude, accuracy);
  }

  @NonNull
  @Override
  public String toString() {
    if (coordinates == null) {
      return "";
    }
    // TODO: i18n.
    return coordinates.getLatitude() + ", " + coordinates.getLongitude();
  }
}
