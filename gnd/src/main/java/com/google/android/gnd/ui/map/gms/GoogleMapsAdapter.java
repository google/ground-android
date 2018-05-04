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

import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gnd.ui.map.MapAdapter;

import io.reactivex.Single;

/** Ground map adapter implementation for Google Maps API. */
public class GoogleMapsAdapter implements MapAdapter {

  private final GoogleMapsFragment fragment;
  private final Single map;

  public GoogleMapsAdapter() {
    this.fragment = new GoogleMapsFragment();
    this.map =
      Single.create(
        source -> fragment.getMapAsync(map -> source.onSuccess(attachViewModel(map))))
        .cache();
  }

  @NonNull
  private GoogleMapsViewModel attachViewModel(GoogleMap map) {
    GoogleMapsViewModel viewModel = ViewModelProviders.of(fragment).get(
      GoogleMapsViewModel.class);
    viewModel.attachMap(map);
    return viewModel;
  }

  @Override
  public Fragment getFragment() {
    return fragment;
  }

  @Override
  public Single<MapViewModel> getViewModel() {
    return map;
  }
}
