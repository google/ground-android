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
import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import android.os.Bundle;
import android.util.Pair;
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
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.persistence.mbtiles.MbtilesFootprintParser;
import com.google.android.gnd.repository.MapsRepository;
import com.google.android.gnd.rx.BooleanOrError;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequestCanceled;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.BottomSheetState;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel.Mode;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapProvider;
import com.google.android.gnd.ui.util.FileUtil;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint
public class MapContainerFragment extends AbstractFragment {

  private static final String MAP_FRAGMENT_KEY = MapProvider.class.getName() + "#fragment";

  @Inject FileUtil fileUtil;
  @Inject MbtilesFootprintParser mbtilesFootprintParser;
  @Inject MapProvider mapProvider;
  @Inject MapsRepository mapsRepository;

  private MapContainerViewModel mapContainerViewModel;
  private HomeScreenViewModel homeScreenViewModel;

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
        .flatMap(MapAdapter::getFeatureClicks)
        .as(disposeOnDestroy(this))
        .subscribe(homeScreenViewModel::onFeatureClick);
    mapAdapter
        .toFlowable()
        .flatMap(MapAdapter::getStartDragEvents)
        .onBackpressureLatest()
        .as(disposeOnDestroy(this))
        .subscribe(__ -> mapContainerViewModel.onMapDrag());
    mapAdapter
        .toFlowable()
        .flatMap(MapAdapter::getCameraMovedEvents)
        .onBackpressureLatest()
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onCameraMove);
    mapAdapter
        .toObservable()
        .flatMap(MapAdapter::getTileProviders)
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::queueTileProvider);

    mapContainerViewModel
        .getConfirmButtonClicks()
        .observe(this, click -> click.ifUnhandled(this::showConfirmationDialog));
    mapContainerViewModel
        .getCancelButtonClicks()
        .observe(this, click -> click.ifUnhandled(__ -> setDefaultMode()));
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    MapContainerFragBinding binding = MapContainerFragBinding.inflate(inflater, container, false);
    binding.setViewModel(mapContainerViewModel);
    binding.setHomeScreenViewModel(homeScreenViewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    disableAddFeatureBtn();

    if (savedInstanceState == null) {
      replaceFragment(R.id.map, mapProvider.getFragment());
    } else {
      mapProvider.restore(restoreChildFragment(savedInstanceState, MAP_FRAGMENT_KEY));
    }
  }

  private void onMapReady(MapAdapter map) {
    Timber.d("MapAdapter ready. Updating subscriptions");
    mapContainerViewModel.setLocationLockEnabled(true);

    // Observe events emitted by the ViewModel.
    mapContainerViewModel.getMapFeatures().observe(this, map::setMapFeatures);
    mapContainerViewModel
        .getLocationLockState()
        .observe(this, state -> onLocationLockStateChange(state, map));
    mapContainerViewModel
        .getCameraUpdateRequests()
        .observe(this, update -> update.ifUnhandled(data -> onCameraUpdate(data, map)));
    mapContainerViewModel.getProjectLoadingState().observe(this, this::onProjectChange);
    homeScreenViewModel
        .getBottomSheetState()
        .observe(this, state -> onBottomSheetStateChange(state, map));
    mapContainerViewModel.getMbtilesFilePaths().observe(this, map::addTileOverlays);
    mapContainerViewModel
        .getSelectMapTypeClicks()
        .observe(
            getViewLifecycleOwner(), action -> action.ifUnhandled(this::showMapTypeSelectorDialog));

    // TODO: Do this the RxJava way
    map.moveCamera(mapContainerViewModel.getCameraPosition().getValue());
    map.setMapType(mapsRepository.getSavedMapType());
  }

  private void showMapTypeSelectorDialog() {
    ImmutableList<Pair<Integer, String>> mapTypes = mapProvider.getMapTypes();
    ImmutableList<Integer> typeNos = stream(mapTypes).map(p -> p.first).collect(toImmutableList());
    int selectedIdx = typeNos.indexOf(mapProvider.getMapType());
    String[] labels = stream(mapTypes).map(p -> p.second).toArray(String[]::new);
    new AlertDialog.Builder(getContext())
        .setTitle(R.string.select_map_type)
        .setSingleChoiceItems(
            labels,
            selectedIdx,
            (dialog, which) -> {
              int mapType = typeNos.get(which);
              mapProvider.setMapType(mapType);
              mapsRepository.saveMapType(mapType);
              dialog.dismiss();
            })
        .setCancelable(true)
        .create()
        .show();
  }

  private void showConfirmationDialog(Point point) {
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.move_point_confirmation)
        .setPositiveButton(android.R.string.ok, (dialog, which) -> moveToNewPosition(point))
        .setNegativeButton(android.R.string.cancel, (dialog, which) -> setDefaultMode())
        .setCancelable(true)
        .create()
        .show();
  }

  private void moveToNewPosition(Point point) {
    Optional<Feature> feature = mapContainerViewModel.getReposFeature();
    if (feature.isEmpty()) {
      Timber.e("Move point failed: No feature selected");
      return;
    }
    if (!(feature.get() instanceof PointFeature)) {
      Timber.e("Only point features can be moved");
      return;
    }
    PointFeature newFeature = ((PointFeature) feature.get()).toBuilder().setPoint(point).build();
    homeScreenViewModel.updateFeature(newFeature);
  }

  private void onBottomSheetStateChange(BottomSheetState state, MapAdapter map) {
    mapContainerViewModel.setSelectedFeature(state.getFeature());
    switch (state.getVisibility()) {
      case VISIBLE:
        map.disable();
        // TODO(#358): Once polygon drawing is implemented, pan & zoom to polygon when
        // selected. This will involve calculating centroid and possibly zoom level based on
        // vertices.
        state
            .getFeature()
            .filter(Feature::isPoint)
            .map(PointFeature.class::cast)
            .ifPresent(
                feature -> {
                  mapContainerViewModel.panAndZoomCamera(feature.getPoint());
                });
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

  private void enableAddFeatureBtn() {
    mapContainerViewModel.setFeatureButtonBackgroundTint(R.color.colorMapAccent);
  }

  private void disableAddFeatureBtn() {
    // NOTE: We don't call addFeatureBtn.setEnabled(false) here since calling it before the fab is
    // shown corrupts its padding when used with useCompatPadding="true".
    mapContainerViewModel.setFeatureButtonBackgroundTint(R.color.colorGrey500);
  }

  private void onLocationLockStateChange(BooleanOrError result, MapAdapter map) {
    result.error().ifPresent(this::onLocationLockError);
    if (result.isTrue()) {
      Timber.d("Location lock enabled");
      map.enableCurrentLocationIndicator();
    } else {
      Timber.d("Location lock disabled");
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
    if (update.getZoomLevel().isPresent()) {
      float zoomLevel = update.getZoomLevel().get();
      if (!update.isAllowZoomOut()) {
        zoomLevel = Math.max(zoomLevel, map.getCurrentZoomLevel());
      }
      map.moveCamera(update.getCenter(), zoomLevel);
    } else {
      map.moveCamera(update.getCenter());
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    saveChildFragment(outState, mapProvider.getFragment(), MAP_FRAGMENT_KEY);
  }

  @Override
  public void onDestroy() {
    mapContainerViewModel.closeProviders();
    super.onDestroy();
  }

  public void setDefaultMode() {
    mapContainerViewModel.setViewMode(Mode.DEFAULT);
    mapContainerViewModel.setReposFeature(Optional.empty());
  }

  public void setRepositionMode(Optional<Feature> feature) {
    mapContainerViewModel.setViewMode(Mode.REPOSITION);
    mapContainerViewModel.setReposFeature(feature);

    Toast.makeText(getContext(), R.string.move_point_hint, Toast.LENGTH_SHORT).show();
  }
}
