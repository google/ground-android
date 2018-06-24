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

package com.google.android.gnd.ui.browse;

import android.support.annotation.Nullable;

import com.google.android.gnd.vo.Place;

public class PlaceSheetState {

  public enum Visibility {
    VISIBLE,
    HIDDEN
  }

  private final Visibility visibility;
  @Nullable
  private Place place;
  @Nullable
  private String title;
  @Nullable
  private String subtitle;

  private PlaceSheetState(Visibility visibility, Place place) {
    this.visibility = visibility;
    this.place = place;
    String caption = place.getCaption();
    String placeTypeLabel = place.getPlaceType().getItemLabel();
    this.title = caption.isEmpty() ? placeTypeLabel : caption;
    this.subtitle = caption.isEmpty() ? "" : placeTypeLabel + " " + place.getCustomId();
  }

  private PlaceSheetState(Visibility visibility) {
    this.visibility = visibility;
  }

  public static PlaceSheetState visible(Place place) {
    return new PlaceSheetState(Visibility.VISIBLE, place);
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

  public String getTitle() {
    return title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public boolean isVisible() {
    return Visibility.VISIBLE.equals(visibility);
  }
}
