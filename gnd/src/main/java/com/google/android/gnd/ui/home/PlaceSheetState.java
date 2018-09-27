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

import android.support.annotation.Nullable;
import com.google.android.gnd.vo.Place;

public class PlaceSheetState {

  public enum Visibility {
    VISIBLE,
    HIDDEN
  }

  private final Visibility visibility;
  private final boolean newPlace;

  @Nullable private Place place;

  private PlaceSheetState(Visibility visibility, Place place, boolean newPlace) {
    this.visibility = visibility;
    this.place = place;
    this.newPlace = newPlace;
  }

  private PlaceSheetState(Visibility visibility) {
    this(visibility, null, false);
  }

  public static PlaceSheetState visible(Place place) {
    return PlaceSheetState.visible(place, false);
  }

  public static PlaceSheetState visible(Place place, boolean addRecord) {
    return new PlaceSheetState(Visibility.VISIBLE, place, addRecord);
  }

  public static PlaceSheetState hidden() {
    return new PlaceSheetState(Visibility.HIDDEN);
  }

  public Place getPlace() {
    return place;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public boolean isVisible() {
    return Visibility.VISIBLE.equals(visibility);
  }

  public boolean isNewPlace() {
    return newPlace;
  }
}
