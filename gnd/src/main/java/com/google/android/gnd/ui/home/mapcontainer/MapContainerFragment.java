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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import butterknife.BindView;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.MapContainerFragBinding;
import com.google.android.gnd.repository.Persistable;
import com.google.android.gnd.rx.BooleanResult;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequestCanceled;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.FeatureSheetState;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.ui.map.MapProvider;
import com.google.android.gnd.ui.map.MapProvider.MapAdapter;
import com.google.android.gnd.vo.Project;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import io.reactivex.Single;
import javax.inject.Inject;

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
public class MapContainerFragment extends AbstractFragment {
  private static final String TAG = MapContainerFragment.class.getSimpleName();
  private static final String MAP_FRAGMENT_KEY = MapProvider.class.getName() + "#fragment";

  @Inject MapProvider mapProvider;

  @BindView(R.id.hamburger_btn)
  ImageButton hamburgerBtn;

  @BindView(R.id.add_feature_btn)
  FloatingActionButton addFeatureBtn;

  @BindView(R.id.location_lock_btn)
  FloatingActionButton locationLockBtn;

  @BindView(R.id.map_btn_layout)
  ViewGroup mapBtnLayout;

  private MapContainerViewModel mapContainerViewModel;
  private HomeScreenViewModel homeScreenViewModel;
  private MainViewModel mainViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mapContainerViewModel = getViewModel(MapContainerViewModel.class);
    homeScreenViewModel = getViewModel(HomeScreenViewModel.class);
    mainViewModel = getViewModel(MainViewModel.class);
    Single<MapAdapter> mapAdapter = mapProvider.getMapAdapter();
    mapAdapter.as(autoDisposable(this)).subscribe(this::onMapReady);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getMarkerClicks)
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onMarkerClick);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getMarkerClicks)
        .as(disposeOnDestroy(this))
        .subscribe(homeScreenViewModel::onMarkerClick);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getDragInteractions)
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onMapDrag);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getCameraPosition)
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onCameraMove);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    MapContainerFragBinding binding = MapContainerFragBinding.inflate(inflater, container, false);
    binding.setViewModel(mapContainerViewModel);
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
    mapContainerViewModel.getFeatures().observe(this, map::updateMarkers);
    mapContainerViewModel
        .getLocationLockState()
        .observe(this, state -> onLocationLockStateChange(state, map));
    mapContainerViewModel
        .getCameraUpdateRequests()
        .observe(this, update -> onCameraUpdate(update, map));
    mapContainerViewModel.getActiveProject().observe(this, this::onProjectChange);
    homeScreenViewModel
        .getFeatureSheetState()
        .observe(this, state -> onFeatureSheetStateChange(state, map));
    addFeatureBtn.setOnClickListener(
        __ -> homeScreenViewModel.onAddFeatureBtnClick(map.getCenter()));
    enableLocationLockBtn();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mainViewModel.getWindowInsets().observe(this, this::onApplyWindowInsets);
  }

  private void onFeatureSheetStateChange(FeatureSheetState state, MapAdapter map) {
    switch (state.getVisibility()) {
      case VISIBLE:
        map.disable();
        mapContainerViewModel.panAndZoomCamera(state.getFeature().getPoint());
        break;
      case HIDDEN:
        map.enable();
        break;
    }
  }

  private void onProjectChange(Persistable<Project> project) {
    if (project.isLoaded()) {
      enableAddFeatureBtn();
    } else {
      disableAddFeatureBtn();
    }
  }

  private void enableLocationLockBtn() {
    locationLockBtn.setEnabled(true);
  }

  private void enableAddFeatureBtn() {
    addFeatureBtn.setBackgroundTintList(
        ColorStateList.valueOf(getResources().getColor(R.color.colorMapAccent)));
  }

  private void disableAddFeatureBtn() {
    // NOTE: We don't call addFeatureBtn.setEnabled(false) here since calling it before the fab is
    // shown corrupts its padding when used with useCompatPadding="true".
    addFeatureBtn.setBackgroundTintList(
        ColorStateList.valueOf(getResources().getColor(R.color.colorGrey500)));
  }

  private void onLocationLockStateChange(BooleanResult result, MapAdapter map) {
    result.error().ifPresent(this::onLocationLockError);
    if (result.isTrue()) {
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
