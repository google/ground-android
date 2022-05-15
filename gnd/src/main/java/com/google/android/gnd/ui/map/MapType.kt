/*
 * Copyright 2021 Google LLC
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

import androidx.annotation.StringRes;

/**
 * MapType refers to the basemap shown below map features and offline satellite imagery. It's called
 * "map styles" in Mapbox and "basemaps" in Leaflet.
 */
public class MapType {

  private final int type;
  private final @StringRes int labelId;

  public MapType(int type, @StringRes int labelId) {
    this.type = type;
    this.labelId = labelId;
  }

  public int getLabelId() {
    return labelId;
  }

  public int getType() {
    return type;
  }
}
