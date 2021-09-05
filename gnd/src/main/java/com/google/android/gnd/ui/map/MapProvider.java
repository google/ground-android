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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.map.gms.GoogleMapsFragment;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import java8.util.function.Supplier;
import javax.inject.Inject;

/**
 * Creates a new {@link MapFragment}. Currently only {@link GoogleMapsFragment} is present, but the
 * goal is to choose it based on user's preference.
 */
public class MapProvider {

  private static final BasemapSource[] BASEMAP_SOURCES = initSources();

  private static BasemapSource[] initSources() {
    // TODO(#711): Allow user to select language and use here.
    return new BasemapSource[] {
      new BasemapSource(
          GoogleMapsFragment::new,
          ImmutableList.<MapType>builder()
              .add(new MapType(GoogleMap.MAP_TYPE_NORMAL, "Normal"))
              .add(new MapType(GoogleMap.MAP_TYPE_SATELLITE, "Satellite"))
              .add(new MapType(GoogleMap.MAP_TYPE_TERRAIN, "Terrain"))
              .add(new MapType(GoogleMap.MAP_TYPE_HYBRID, "Hybrid"))
              .build())
    };
  }

  public static BasemapSource getSource() {
    // TODO: Select based on user preference.
    return BASEMAP_SOURCES[0];
  }

  @Hot private final SingleSubject<MapAdapter> map = SingleSubject.create();

  @Inject
  public MapProvider() {}

  public MapFragment createFragment() {
    return getSource().supplier.get();
  }

  public void setMapAdapter(MapAdapter adapter) {
    map.onSuccess(adapter);
  }

  public Single<MapAdapter> getMapAdapter() {
    return map;
  }

  public int getMapType() {
    return map.getValue().getMapType();
  }

  // TODO(#714): Use enum instead of int to represent basemap types.
  public void setMapType(int mapType) {
    map.getValue().setMapType(mapType);
  }

  public ImmutableList<MapType> getMapTypes() {
    return getSource().mapTypes;
  }

  private static class BasemapSource {

    private final Supplier<? extends MapFragment> supplier;
    private final ImmutableList<MapType> mapTypes;

    private BasemapSource(
        Supplier<? extends MapFragment> supplier, ImmutableList<MapType> mapTypes) {
      this.supplier = supplier;
      this.mapTypes = mapTypes;
    }
  }

  /**
   * MapType refers to the basemap shown below map features and offline satellite imagery. It's
   * called "map styles" in Mapbox and "basemaps" in Leaflet.
   */
  public static class MapType {
    public final int type;
    public final String label;

    MapType(int type, String label) {
      this.type = type;
      this.label = label;
    }
  }
}
