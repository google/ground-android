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

package com.google.android.gnd.ui.map.gms;

import android.util.Pair;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.MarkerIconFactory;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapProvider;
import com.google.android.gnd.ui.util.BitmapUtil;
import com.google.common.collect.ImmutableList;
import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import javax.inject.Inject;

/** Ground map adapter implementation for Google Maps API. */
public class GoogleMapsMapProvider implements MapProvider {

  private final MarkerIconFactory markerIconFactory;
  private final BitmapUtil bitmapUtil;
  @Hot private final SingleSubject<MapAdapter> map = SingleSubject.create();

  @SuppressWarnings("NullAway.Init")
  private GoogleMapsFragment fragment;

  @Inject
  public GoogleMapsMapProvider(MarkerIconFactory markerIconFactory, BitmapUtil bitmapUtil) {
    this.markerIconFactory = markerIconFactory;
    this.bitmapUtil = bitmapUtil;
  }

  @Override
  public void restore(Fragment fragment) {
    init((GoogleMapsFragment) fragment);
  }

  private void init(GoogleMapsFragment fragment) {
    if (this.fragment != null) {
      throw new IllegalStateException("Map fragment already initialized");
    }
    this.fragment = fragment;
    createMapAsync();
  }

  private void createMapAsync() {
    ((GoogleMapsFragment) getFragment())
        .getMapAsync(
            googleMap ->
                map.onSuccess(
                    new GoogleMapsMapAdapter(
                        googleMap, fragment.getContext(), markerIconFactory, bitmapUtil)));
  }

  @Override
  public Fragment getFragment() {
    if (fragment == null) {
      init(new GoogleMapsFragment());
    }
    return fragment;
  }

  @Override
  public Single<MapAdapter> getMapAdapter() {
    return map;
  }

  @Override
  public int getMapType() {
    return map.getValue().getMapType();
  }

  @Override
  public void setMapType(int mapType) {
    map.getValue().setMapType(mapType);
  }

  @Override
  public ImmutableList<Pair<Integer, String>> getMapTypes() {
    // TODO(#711): Allow user to select language and use here.
    return ImmutableList.<Pair<Integer, String>>builder()
        .add(new Pair<>(GoogleMap.MAP_TYPE_NORMAL, "Normal"))
        .add(new Pair<>(GoogleMap.MAP_TYPE_SATELLITE, "Satellite"))
        .add(new Pair<>(GoogleMap.MAP_TYPE_TERRAIN, "Terrain"))
        .add(new Pair<>(GoogleMap.MAP_TYPE_HYBRID, "Hybrid"))
        .build();
  }
}
