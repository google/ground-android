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

package com.google.android.gnd.ui.map.gms;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.map.MapFragment;
import com.google.android.gnd.ui.map.MapFragmentFactory;
import com.google.android.gnd.ui.map.MapType;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;

public class GoogleMapsMapFragmentFactory implements MapFragmentFactory {

  @Inject
  GoogleMapsMapFragmentFactory() {}

  @Override
  public MapFragment createFragment() {
    return new GoogleMapsFragment();
  }

  @Override
  public ImmutableList<MapType> getMapTypes() {
    return ImmutableList.<MapType>builder()
        .add(new MapType(GoogleMap.MAP_TYPE_NORMAL, R.string.normal))
        .add(new MapType(GoogleMap.MAP_TYPE_SATELLITE, R.string.satellite))
        .add(new MapType(GoogleMap.MAP_TYPE_TERRAIN, R.string.terrain))
        .add(new MapType(GoogleMap.MAP_TYPE_HYBRID, R.string.hybrid))
        .build();
  }
}
