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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.MapContainerFragBinding;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.rx.BooleanOrError;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequestCanceled;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.BottomSheetState;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapProvider;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import javax.inject.Inject;
import timber.log.Timber;

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint
public class MapContainerFragment extends AbstractFragment {
  private static final String MAP_FRAGMENT_KEY = MapProvider.class.getName() + "#fragment";

  @Inject MapProvider mapProvider;

  private MapContainerViewModel mapContainerViewModel;
  private HomeScreenViewModel homeScreenViewModel;
  private MapContainerFragBinding binding;

  private void showMapTypeSelectorDialog() {
    new AlertDialog.Builder(getContext())
        .setTitle(R.string.select_map_type)
        .setSingleChoiceItems(
            mapProvider.getMapTypes().values().toArray(new String[0]),
            mapProvider.getMapType(),
            (dialog, which) -> {
              mapProvider.setMapType(which);
              dialog.dismiss();
            })
        .setCancelable(true)
        .create()
        .show();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mapContainerViewModel = getViewModel(MapContainerViewModel.class);
    homeScreenViewModel = getViewModel(HomeScreenViewModel.class);
    Single<MapAdapter> mapAdapter = mapProvider.getMapAdapter();
    mapAdapter.as(autoDisposable(this)).subscribe(this::onMapReady);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getMapPinClicks)
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onMarkerClick);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getMapPinClicks)
        .as(disposeOnDestroy(this))
        .subscribe(homeScreenViewModel::onMarkerClick);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getDragInteractions)
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onMapDrag);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getCameraMoves)
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onCameraMove);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getTileProviders)
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::queueTileProvider);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = MapContainerFragBinding.inflate(inflater, container, false);
    binding.setViewModel(mapContainerViewModel);
    binding.setHomeScreenViewModel(homeScreenViewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    disableAddFeatureBtn();

    if (savedInstanceState == null) {
      replaceFragment(R.id.map, mapProvider.getFragment());
    } else {
      mapProvider.restore(restoreChildFragment(savedInstanceState, MAP_FRAGMENT_KEY));
    }

    mapContainerViewModel
        .getShowMapTypeSelectorRequests()
        .observe(getViewLifecycleOwner(), __ -> showMapTypeSelectorDialog());
  }

  private void onMapReady(MapAdapter map) {
    Timber.d("MapAdapter ready. Updating subscriptions");
    // Observe events emitted by the ViewModel.
    mapContainerViewModel.getMapPins().observe(this, map::setMapPins);
    mapContainerViewModel
        .getLocationLockState()
        .observe(this, state -> onLocationLockStateChange(state, map));
    mapContainerViewModel
        .getCameraUpdateRequests()
        .observe(this, update -> onCameraUpdate(update, map));
    mapContainerViewModel.getActiveProject().observe(this, this::onProjectChange);
    homeScreenViewModel
        .getBottomSheetState()
        .observe(this, state -> onBottomSheetStateChange(state, map));
    binding.addFeatureBtn.setOnClickListener(
        __ -> homeScreenViewModel.onAddFeatureBtnClick(map.getCameraTarget()));
    enableLocationLockBtn();
    mapContainerViewModel.getMbtilesFilePaths().observe(this, map::addTileOverlays);
  }

  private void onBottomSheetStateChange(BottomSheetState state, MapAdapter map) {
    switch (state.getVisibility()) {
      case VISIBLE:
        map.disable();
        mapContainerViewModel.panAndZoomCamera(state.getFeature().getPoint());
        break;
      case HIDDEN:
        map.enable();
        break;
      default:
        Timber.e("Unhandled visibility: %s", state.getVisibility());
        break;
    }
  }

  private void onProjectChange(Loadable<Project> project) {
    if (project.isLoaded()) {
      enableAddFeatureBtn();
    } else {
      disableAddFeatureBtn();
    }
  }

  private void enableLocationLockBtn() {
    binding.locationLockBtn.setEnabled(true);
  }

  private void enableAddFeatureBtn() {
    binding.addFeatureBtn.setBackgroundTintList(
        ColorStateList.valueOf(getResources().getColor(R.color.colorMapAccent)));
  }

  private void disableAddFeatureBtn() {
    // NOTE: We don't call addFeatureBtn.setEnabled(false) here since calling it before the fab is
    // shown corrupts its padding when used with useCompatPadding="true".
    binding.addFeatureBtn.setBackgroundTintList(
        ColorStateList.valueOf(getResources().getColor(R.color.colorGrey500)));
  }

  private void onLocationLockStateChange(BooleanOrError result, MapAdapter map) {
    result.error().ifPresent(this::onLocationLockError);
    if (result.isTrue()) {
      Timber.d("Location lock enabled");
      map.enableCurrentLocationIndicator();
      binding.locationLockBtn.setImageResource(R.drawable.ic_gps_blue);
    } else {
      Timber.d("Location lock disabled");
      binding.locationLockBtn.setImageResource(R.drawable.ic_gps_grey600);
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

  private void showUserActionFailureMessage(@StringRes int resId) {
    Toast.makeText(getContext(), resId, Toast.LENGTH_LONG).show();
  }

  private void onCameraUpdate(MapContainerViewModel.CameraUpdate update, MapAdapter map) {
    Timber.v("Update camera: %s", update);
    if (update.getMinZoomLevel().isPresent()) {
      map.moveCamera(
          update.getCenter(), Math.max(update.getMinZoomLevel().get(), map.getCurrentZoomLevel()));
    } else {
      map.moveCamera(update.getCenter());
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    saveChildFragment(outState, mapProvider.getFragment(), MAP_FRAGMENT_KEY);
  }

  @Override
  public void onDestroy() {
    mapContainerViewModel.closeProviders();
    super.onDestroy();
  }
}
