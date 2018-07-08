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

package com.google.android.gnd.ui.browse.mapcontainer;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.WindowInsetsCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import butterknife.BindView;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequestCanceled;
import com.google.android.gnd.ui.browse.BrowseViewModel;
import com.google.android.gnd.ui.browse.PlaceSheetState;
import com.google.android.gnd.ui.browse.mapcontainer.MapContainerViewModel.LocationLockStatus;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.map.MapProvider;
import com.google.android.gnd.ui.map.MapProvider.MapAdapter;
import com.google.android.gnd.vo.Project;
import com.jakewharton.rxbinding2.view.RxView;
import javax.inject.Inject;

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
public class MapContainerFragment extends AbstractFragment {
  private static final String TAG = MapContainerFragment.class.getSimpleName();

  // TODO: Get ViewModel from ViewModelFactory instead.
  @Inject ViewModelProvider.Factory viewModelFactory;

  @Inject MapProvider mapAdapter;

  @BindView(R.id.add_place_btn)
  FloatingActionButton addPlaceBtn;

  @BindView(R.id.location_lock_btn)
  FloatingActionButton locationLockBtn;

  @BindView(R.id.map_btn_layout)
  ViewGroup mapBtnLayout;

  private MapContainerViewModel mapContainerViewModel;
  private BrowseViewModel browseViewModel;
  private MainViewModel mainViewModel;

  @Inject
  public MapContainerFragment() {}

  @Override
  protected void obtainViewModels() {
    mapContainerViewModel =
        ViewModelProviders.of(this, viewModelFactory).get(MapContainerViewModel.class);
    browseViewModel =
        ViewModelProviders.of(getActivity(), viewModelFactory).get(BrowseViewModel.class);
    mainViewModel = ViewModelProviders.of(getActivity(), viewModelFactory).get(MainViewModel.class);
  }

  @Override
  protected View createView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.map_container_frag, container, false);
  }

  @Override
  protected void setUpView() {
    disableLocationLockBtn();
    disableAddPlaceBtn();
    replaceFragment(R.id.map, mapAdapter.getFragment());
  }

  @Override
  protected void observeViewModels() {
    mainViewModel.getWindowInsets().observe(this, this::onApplyWindowInsets);
    mapAdapter.getMapAdapter().as(autoDisposable(this)).subscribe(this::onMapReady);
  }

  private void onMapReady(MapAdapter map) {
    Log.d(TAG, "MapAdapter ready. Updating subscriptions");
    // Observe events emitted by the ViewModel.
    mapContainerViewModel.getPlaces().observe(this, map::updateMarkers);
    mapContainerViewModel
        .getLocationLockStatus()
        .observe(this, status -> onLocationLockStatusChange(status, map));
    mapContainerViewModel.getCameraUpdates().observe(this, update -> onCameraUpdate(update, map));
    mapContainerViewModel.getActiveProject().observe(this, this::onProjectChange);
    browseViewModel
        .getPlaceSheetState()
        .observe(this, state -> onPlaceSheetStateChange(state, map));
    // TODO: Use Butterknife here instead for better readability/less boilerplate.
    RxView.clicks(addPlaceBtn)
        .as(autoDisposable(this))
        .subscribe(__ -> browseViewModel.onAddPlaceBtnClick(map.getCenter()));
    RxView.clicks(locationLockBtn)
        .as(autoDisposable(this))
        .subscribe(__ -> onLocationLockClick(map));
    map.getMarkerClicks().as(autoDisposable(this)).subscribe(mapContainerViewModel::onMarkerClick);
    map.getMarkerClicks().as(autoDisposable(this)).subscribe(browseViewModel::onMarkerClick);
    map.getDragInteractions().as(autoDisposable(this)).subscribe(mapContainerViewModel::onMapDrag);
    enableLocationLockBtn();
  }

  public void onLocationLockClick(MapAdapter map) {
    if (mapContainerViewModel.isLocationLockEnabled()) {
      mapContainerViewModel.disableLocationLock().as(autoDisposable(this)).subscribe();
    } else {
      mapContainerViewModel.enableLocationLock().as(autoDisposable(this)).subscribe();
    }
  }

  private void onPlaceSheetStateChange(PlaceSheetState state, MapAdapter map) {
    switch (state.getVisibility()) {
      case VISIBLE:
        map.disable();
        break;
      case HIDDEN:
        map.enable();
        break;
    }
  }

  private void onProjectChange(Resource<Project> project) {
    if (project.isLoaded()) {
      enableAddPlaceBtn();
    } else {
      disableAddPlaceBtn();
    }
  }

  private void enableLocationLockBtn() {
    locationLockBtn.setEnabled(true);
  }

  private void disableLocationLockBtn() {
    locationLockBtn.setEnabled(false);
  }

  private void enableAddPlaceBtn() {
    addPlaceBtn.setBackgroundTintList(
        ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
  }

  private void disableAddPlaceBtn() {
    // NOTE: We don't call addPlaceBtn.setEnabled(false) here since calling it before the fab is
    // shown corrupts its padding when used with useCompatPadding="true".
    addPlaceBtn.setBackgroundTintList(
        ColorStateList.valueOf(getResources().getColor(R.color.colorGrey500)));
  }

  private void onLocationLockStatusChange(LocationLockStatus status, MapAdapter map) {
    if (status.isError()) {
      onLocationLockError(status.getError());
    }
    if (status.isEnabled()) {
      Log.d(TAG, "Location lock enabled");
      map.enableCurrentLocationIndicator();
      locationLockBtn.setImageResource(R.drawable.ic_gps_blue);
    } else {
      Log.d(TAG, "Location lock disabled");
      locationLockBtn.setImageResource(R.drawable.ic_gps_grey600);
    }
  }

  private void onLocationLockError(Throwable t) {
    if (t instanceof PermissionDeniedException) {
      showUserActionFailureMessage(R.string.no_fine_location_permissions);
    } else if (t instanceof SettingsChangeRequestCanceled) {
      showUserActionFailureMessage(R.string.location_disabled_in_settings);
    } else {
      showUserActionFailureMessage(R.string.location_updates_unknown_error);
    }
  }

  private void showUserActionFailureMessage(int resId) {
    Toast.makeText(getContext(), resId, Toast.LENGTH_LONG).show();
  }

  private void onCameraUpdate(MapContainerViewModel.CameraUpdate update, MapAdapter map) {
    Log.d(TAG, "Update camera: " + update);
    if (update.getMinZoomLevel().isPresent()) {
      map.moveCamera(
          update.getCenter(), Math.max(update.getMinZoomLevel().get(), map.getCurrentZoomLevel()));
    } else {
      map.moveCamera(update.getCenter());
    }
  }

  private void onApplyWindowInsets(WindowInsetsCompat windowInsets) {
    mapBtnLayout.setTranslationY(-windowInsets.getSystemWindowInsetBottom());
  }
}
