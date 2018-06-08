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

package com.google.android.gnd.ui.mapcontainer;

import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Point;

class MarkerUpdate {

  enum Type {
    CLEAR_ALL,
    ADD_OR_UPDATE_MARKER,
    REMOVE_MARKER,
    INVALID
  }

  private Type type;
  private String id;
  private Point point;
  private String iconId;
  private int iconColor;
  private Place place;
  private boolean hasPendingWrites;

  private MarkerUpdate(Type type) {
    this.type = type;
  }

  public static MarkerUpdate clearAll() {
    return new MarkerUpdate(Type.CLEAR_ALL);
  }

  public static MarkerUpdate invalid() {
    return new MarkerUpdate(Type.INVALID);
  }

  public static MarkerUpdate addOrUpdatePlace(
    Place place, String iconId, int iconColor, boolean hasPendingWrites) {
    MarkerUpdate u = new MarkerUpdate(Type.ADD_OR_UPDATE_MARKER);
    // TODO: Stop attaching Place to map markers.
    u.place = place;
    u.id = place.getId();
    u.point = place.getPoint();
    u.iconId = iconId;
    u.iconColor = iconColor;
    u.hasPendingWrites = hasPendingWrites;
    return u;
  }

  public static MarkerUpdate remove(String id) {
    MarkerUpdate u = new MarkerUpdate(Type.REMOVE_MARKER);
    u.id = id;
    return u;
  }

  public String getId() {
    return id;
  }

  public Point getPoint() {
    return point;
  }

  public Place getPlace() {
    return place;
  }

  public boolean hasPendingWrites() {
    return hasPendingWrites;
  }

  public Type getType() {
    return type;
  }

  public String getIconId() {
    return iconId;
  }

  public int getIconColor() {
    return iconColor;
  }

  public boolean isValid() {
    return !Type.INVALID.equals(type);
  }
}
