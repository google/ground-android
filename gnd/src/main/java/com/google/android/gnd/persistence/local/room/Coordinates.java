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

package com.google.android.gnd.persistence.local.room;

import androidx.annotation.NonNull;
import com.google.android.gnd.vo.Point;

/**
 * Represents a lat/lng coordinate in {@link
 * com.google.android.gnd.persistence.shared.FeatureMutation} instances.
 */
public class Coordinates {

  @NonNull public double latitude;

  @NonNull public double longitude;

  @NonNull
  public static Coordinates fromPoint(Point point) {
    Coordinates c = new Coordinates();
    c.latitude = point.getLatitude();
    c.longitude = point.getLongitude();
    return c;
  }
}
