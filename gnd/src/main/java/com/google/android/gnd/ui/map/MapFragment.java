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

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import com.google.android.gnd.AbstractGndFragment;
import com.google.android.gnd.R;
import com.google.android.gnd.model.PlaceIcon;
import javax.inject.Inject;

public class MapFragment extends AbstractGndFragment {

  @Inject
  MapViewModelFactory viewModelFactory;

  @BindView(R.id.map)
  GoogleMapsView mapView;

  private MapViewModel viewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapViewModel.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_map, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.getMap().subscribe(this::onMapReady);
  }

  private void onMapReady(GoogleMapImpl map) {
    viewModel.mapMarkers().observe(this, update -> onMarkerUpdate(map, update));
  }

  private void onMarkerUpdate(GoogleMapImpl map, MarkerUpdate update) {
    switch (update.getType()) {
      case CLEAR_ALL:
        map.removeAllMarkers();
        break;
      case ADD_OR_UPDATE_MARKER:
        PlaceIcon
            icon =
            new PlaceIcon(getContext(), update.getIconId(), update.getIconColor());
        map.addOrUpdateMarker(
            new MapMarker<>(update.getId(),
                update.getPlace().getPoint(),
                icon,
                update.getPlace()),
            update.hasPendingWrites(),
            false);
        break;
      case REMOVE_MARKER:
        map.removeMarker(update.getId());
        break;
    }
  }
}
