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

package com.google.android.gnd.ui.map;

import androidx.annotation.Nullable;
import com.google.android.gnd.model.locationofinterest.LocationOfInterest;
import com.google.android.gnd.model.locationofinterest.Point;
import com.google.android.gnd.model.job.Style;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MapPin extends MapLocationOfInterest {

  public static Builder newBuilder() {
    return new AutoValue_MapPin.Builder();
  }

  public abstract String getId();

  public abstract Point getPosition();

  public abstract Style getStyle();

  // TODO: Stop embedding entire LocationOfInterest in pins to free up memory. Instead, copy only details
  // relevant to rendering pins and uuid to reference the related LocationOfInterest.
  @Nullable
  @Override
  public abstract LocationOfInterest getLocationOfInterest();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setPosition(Point newPosition);

    public abstract Builder setStyle(Style style);

    public abstract Builder setLocationOfInterest(@Nullable LocationOfInterest newLocationOfInterest);

    public abstract MapPin build();
  }
}
