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

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.google.android.gnd.ui.map.MapProvider;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

/** Ground map adapter implementation for Google Maps API. */
public class GoogleMapsMapProvider implements MapProvider {

  @Nullable
  private GoogleMapsFragment fragment;
  @Nullable
  private Single<MapAdapter> map;

  @Override
  public void restore(Fragment fragment) {
    init((GoogleMapsFragment) fragment);
  }

  private void init(GoogleMapsFragment fragment) {
    if (this.fragment != null) {
      throw new IllegalStateException("Map fragment already initialized");
    }
    this.fragment = fragment;
    this.map = Single.create(this::createMapAsync).cache();
  }

  private void createMapAsync(SingleEmitter<MapAdapter> emitter) {
    ((GoogleMapsFragment) getFragment()).getMapAsync(
        googleMap -> emitter.onSuccess(new GoogleMapsMapAdapter(googleMap, fragment.getContext())));
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
    if (map == null) {
      throw new IllegalStateException("Called before provider initialized");
    }
    return map;
  }
}
