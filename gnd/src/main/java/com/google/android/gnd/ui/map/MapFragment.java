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
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import com.google.android.gnd.AbstractGndFragment;
import com.google.android.gnd.R;
import com.google.android.gnd.model.PlaceIcon;
import com.google.android.gnd.model.Point;
import com.google.android.gnd.ui.map.MapAdapter.Map;
import com.google.android.gnd.ui.map.gms.GoogleMapsApiMapAdapter;
import com.jakewharton.rxbinding2.view.RxView;
import javax.inject.Inject;

/**
 * Main app view, displaying the map and related controls (center cross-hairs, add button, etc).
 */
public class MapFragment extends AbstractGndFragment {

  @Inject
  MapViewModelFactory viewModelFactory;

  @Inject
  AddPlaceDialogFragment addPlaceDialogFragment;

  @BindView(R.id.add_place_btn)
  View addPlaceBtn;

  // HACK: Horrible temp workaround for refactor.
  // TODO: Replace with injected field.
  public static MapAdapter mapAdapter;

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
    View view = inflater.inflate(R.layout.fragment_map, container, false);

    FragmentTransaction ft = getFragmentManager().beginTransaction();
//    / HACK: INJECT ME!
    mapAdapter = new GoogleMapsApiMapAdapter();
    ft.replace(R.id.map, mapAdapter.getMapFragment());
    ft.commit();
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    mapAdapter.map().subscribe(this::onMapReady);
  }

  private void onMapReady(Map map) {
    viewModel.mapMarkers().observe(this, update -> onMarkerUpdate(map, update));
    RxView.clicks(addPlaceBtn).subscribe(__ -> showAddPlaceDialog(map.getCenter()));
  }

  private void showAddPlaceDialog(Point location) {
    addPlaceDialogFragment
        .show(getFragmentManager(), location)
        .subscribe(viewModel::onAddPlace);
  }

  private void onMarkerUpdate(Map map, MarkerUpdate update) {
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
