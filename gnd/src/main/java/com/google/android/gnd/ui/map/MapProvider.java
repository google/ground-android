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

import androidx.annotation.StringRes;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gnd.R;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.map.gms.GoogleMapsFragment;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import java8.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Creates a new {@link MapFragment}. Currently only {@link GoogleMapsFragment} is present, but the
 * goal is to choose it based on user's preference.
 */
@Singleton
public class MapProvider {

  private static final BasemapSource[] BASEMAP_SOURCES = initSources();

  @Hot private final FlowableProcessor<MapAdapter> map = PublishProcessor.create();

  @Inject
  public MapProvider() {}

  private static BasemapSource[] initSources() {
    return new BasemapSource[] {
      new BasemapSource(
          GoogleMapsFragment::new,
          ImmutableList.<MapType>builder()
              .add(new MapType(GoogleMap.MAP_TYPE_NORMAL, R.string.normal))
              .add(new MapType(GoogleMap.MAP_TYPE_SATELLITE, R.string.satellite))
              .add(new MapType(GoogleMap.MAP_TYPE_TERRAIN, R.string.terrain))
              .add(new MapType(GoogleMap.MAP_TYPE_HYBRID, R.string.hybrid))
              .build())
    };
  }

  public static BasemapSource getSource() {
    // TODO: Select based on user preference.
    return BASEMAP_SOURCES[0];
  }

  public MapFragment createFragment() {
    return getSource().supplier.get();
  }

  public ImmutableList<MapType> getMapTypes() {
    return getSource().mapTypes;
  }

  @Hot
  public Flowable<MapAdapter> getMapAdapter() {
    return map;
  }

  public void setMapAdapter(MapAdapter adapter) {
    map.onNext(adapter);
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
    public final @StringRes int labelId;

    MapType(int type, @StringRes int labelId) {
      this.type = type;
      this.labelId = labelId;
    }
  }
}
