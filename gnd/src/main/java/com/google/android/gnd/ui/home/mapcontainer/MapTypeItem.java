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

package com.google.android.gnd.ui.home.mapcontainer;

import com.google.android.gnd.ui.map.MapFragment;
import com.google.auto.value.AutoValue;

/**
 * A map type item in the list view inside map container.
 */
@AutoValue
public abstract class MapTypeItem {
  public abstract int getType();
  public abstract String getLabel();
  abstract MapFragment getMapFragment();

  public static MapTypeItem.Builder newBuilder() {
    return new AutoValue_MapTypeItem.Builder();
  }

  public boolean isSelected() {
    return getMapFragment().getMapType() == getType();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setType(int type);
    public abstract Builder setLabel(String label);
    public abstract Builder setMapFragment(MapFragment mapFragment);
    public abstract MapTypeItem build();
  }
}
