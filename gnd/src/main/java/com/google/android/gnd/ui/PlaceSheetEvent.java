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

package com.google.android.gnd.ui;

import com.google.android.gnd.vo.Place;

public class PlaceSheetEvent {

  public enum Type {
    SHOW,
    HIDE
  }

  private final Type type;
  private Place place;
  private String title;
  private String subtitle;

  private PlaceSheetEvent(Type type, Place place) {
    this.type = type;
    this.place = place;
    String caption = place.getCaption();
    String placeTypeLabel = place.getPlaceType().getItemLabel();
    this.title = caption.isEmpty() ? placeTypeLabel : caption;
    this.subtitle = caption.isEmpty() ? "" : placeTypeLabel + " " + place.getCustomId();
  }

  private PlaceSheetEvent(Type type) {
    this.type = type;
  }

  public static PlaceSheetEvent show(Place place) {
    return new PlaceSheetEvent(Type.SHOW, place);
  }

  public static PlaceSheetEvent hide() {
    return new PlaceSheetEvent(Type.HIDE);
  }

  public Place getPlace() {
    return place;
  }

  public Type getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public boolean isShowEvent() {
    return type == Type.SHOW;
  }
}
