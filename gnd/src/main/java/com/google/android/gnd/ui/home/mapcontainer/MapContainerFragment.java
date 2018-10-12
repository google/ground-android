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

package com.google.android.gnd.ui.home.mapcontainer;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.rx.RxAutoDispose.disposeOnDestroy;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import butterknife.BindView;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.MapContainerFragBinding;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequestCanceled;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.ui.home.PlaceSheetState;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel.LocationLockStatus;
import com.google.android.gnd.ui.map.MapProvider;
import com.google.android.gnd.ui.map.MapProvider.MapAdapter;
import com.google.android.gnd.vo.Project;
import io.reactivex.subjects.SingleSubject;
import javax.inject.Inject;

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
public class MapContainerFragment extends AbstractFragment {
  private static final String TAG = MapContainerFragment.class.getSimpleName();
  private static final String MAP_FRAGMENT_KEY = MapProvider.class.getName() + "#fragment";

  @Inject MapProvider mapProvider;

  @BindView(R.id.hamburger_btn)
  ImageButton hamburgerBtn;

  @BindView(R.id.add_place_btn)
  FloatingActionButton addPlaceBtn;

  @BindView(R.id.location_lock_btn)
  FloatingActionButton locationLockBtn;

  @BindView(R.id.map_btn_layout)
  ViewGroup mapBtnLayout;

  private MapContainerViewModel mapContainerViewModel;
  private HomeScreenViewModel homeScreenViewModel;
  private MainViewModel mainViewModel;
  private SingleSubject<MapAdapter> map = SingleSubject.create();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mapContainerViewModel = get(MapContainerViewModel.class);
    homeScreenViewModel = get(HomeScreenViewModel.class);
    mainViewModel = get(MainViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    MapContainerFragBinding binding = MapContainerFragBinding.inflate(inflater, container, false);
    binding.setHomeScreenViewModel(homeScreenViewModel);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (savedInstanceState == null) {
      replaceFragment(R.id.map, mapProvider.getFragment());
    } else {
      mapProvider.restore(restoreChildFragment(savedInstanceState, MAP_FRAGMENT_KEY));
    }
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
    homeScreenViewModel
        .getPlaceSheetState()
        .observe(this, state -> onPlaceSheetStateChange(state, map));
    addPlaceBtn.setOnClickListener(__ -> homeScreenViewModel.onAddPlaceBtnClick(map.getCenter()));
    locationLockBtn.setOnClickListener(__ -> onLocationLockClick(map));
    map.getMarkerClicks()
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onMarkerClick);
    map.getMarkerClicks().as(disposeOnDestroy(this)).subscribe(homeScreenViewModel::onMarkerClick);
    map.getDragInteractions()
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onMapDrag);
    map.getCameraPosition()
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onCameraMove);
    enableLocationLockBtn();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mainViewModel.getWindowInsets().observe(this, this::onApplyWindowInsets);
    mapProvider.getMapAdapter().as(autoDisposable(this)).subscribe(this::onMapReady);
  }

  public void onLocationLockClick(MapAdapter map) {
    if (mapContainerViewModel.isLocationLockEnabled()) {
      mapContainerViewModel.disableLocationLock();
    } else {
      mapContainerViewModel.enableLocationLock();
    }
  }

  private void onPlaceSheetStateChange(PlaceSheetState state, MapAdapter map) {
    switch (state.getVisibility()) {
      case VISIBLE:
        map.disable();
        mapContainerViewModel.panAndZoomCamera(state.getPlace().getPoint());
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

  private void enableAddPlaceBtn() {
    addPlaceBtn.setBackgroundTintList(
      ColorStateList.valueOf(getResources().getColor(R.color.colorMapAccent)));
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
    Log.v(TAG, "Update camera: " + update);
    if (update.getMinZoomLevel().isPresent()) {
      map.moveCamera(
          update.getCenter(), Math.max(update.getMinZoomLevel().get(), map.getCurrentZoomLevel()));
    } else {
      map.moveCamera(update.getCenter());
    }
  }

  private void onApplyWindowInsets(WindowInsetsCompat windowInsets) {
    ViewCompat.onApplyWindowInsets(mapProvider.getFragment().getView(), windowInsets);
    hamburgerBtn.setTranslationY(windowInsets.getSystemWindowInsetTop());
    mapBtnLayout.setTranslationY(-windowInsets.getSystemWindowInsetBottom());
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    saveChildFragment(outState, mapProvider.getFragment(), MAP_FRAGMENT_KEY);
  }
}
