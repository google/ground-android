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

package com.google.android.gnd.ui.home;

import androidx.annotation.Nullable;
import com.google.android.gnd.model.locationofinterest.LocationOfInterest;
import com.google.android.gnd.model.locationofinterest.PointOfInterest;
import java8.util.Optional;

public class BottomSheetState {

  public enum Visibility {
    VISIBLE,
    HIDDEN
  }

  private final Visibility visibility;
  private final Optional<LocationOfInterest> locationOfInterest;

  private BottomSheetState(Visibility visibility, @Nullable LocationOfInterest locationOfInterest) {
    this.visibility = visibility;
    this.locationOfInterest = Optional.ofNullable(locationOfInterest);
  }

  private BottomSheetState(Visibility visibility) {
    this(visibility, null);
  }

  public static BottomSheetState visible(LocationOfInterest locationOfInterest) {
    return new BottomSheetState(Visibility.VISIBLE, locationOfInterest);
  }

  public static BottomSheetState hidden() {
    return new BottomSheetState(Visibility.HIDDEN);
  }

  public Optional<LocationOfInterest> getLocationOfInterest() {
    return locationOfInterest;
  }

  public boolean isPointOfInterest() {
    return locationOfInterest.isPresent() && locationOfInterest.get() instanceof PointOfInterest;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public boolean isVisible() {
    return Visibility.VISIBLE.equals(visibility);
  }
}
