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

package com.google.android.ground.ui.home.mapcontainer;

import static androidx.navigation.fragment.NavHostFragment.findNavController;
import static com.google.android.ground.rx.RxAutoDispose.autoDisposable;
import static com.google.android.ground.rx.RxAutoDispose.disposeOnDestroy;
import static java8.util.stream.StreamSupport.stream;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import com.google.android.ground.R;
import com.google.android.ground.databinding.MapContainerFragBinding;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.geometry.GeometryType;
import com.google.android.ground.model.geometry.Point;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.repository.MapsRepository;
import com.google.android.ground.rx.BooleanOrError;
import com.google.android.ground.rx.Loadable;
import com.google.android.ground.system.PermissionDeniedException;
import com.google.android.ground.system.SettingsChangeRequestCanceled;
import com.google.android.ground.ui.common.AbstractMapViewerFragment;
import com.google.android.ground.ui.home.BottomSheetState;
import com.google.android.ground.ui.home.HomeScreenFragmentDirections;
import com.google.android.ground.ui.home.HomeScreenViewModel;
import com.google.android.ground.ui.home.mapcontainer.MapContainerViewModel.Mode;
import com.google.android.ground.ui.map.CameraPosition;
import com.google.android.ground.ui.map.MapFragment;
import com.google.android.ground.ui.map.MapType;
import dagger.hilt.android.AndroidEntryPoint;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint
public class MapContainerFragment extends AbstractMapViewerFragment {

  @Inject MapsRepository mapsRepository;
  PolygonDrawingViewModel polygonDrawingViewModel;
  private MapContainerViewModel mapContainerViewModel;
  private HomeScreenViewModel homeScreenViewModel;
  private MapContainerFragBinding binding;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mapContainerViewModel = getViewModel(MapContainerViewModel.class);
    homeScreenViewModel = getViewModel(HomeScreenViewModel.class);
    LocationOfInterestRepositionViewModel locationOfInterestRepositionViewModel =
        getViewModel(LocationOfInterestRepositionViewModel.class);
    polygonDrawingViewModel = getViewModel(PolygonDrawingViewModel.class);
    getMapFragment()
        .getMapPinClicks()
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onMarkerClick);
    getMapFragment()
        .getMapPinClicks()
        .as(disposeOnDestroy(this))
        .subscribe(homeScreenViewModel::onMarkerClick);
    getMapFragment()
        .getLocationOfInterestClicks()
        .as(disposeOnDestroy(this))
        .subscribe(homeScreenViewModel::onLocationOfInterestClick);
    getMapFragment()
        .getStartDragEvents()
        .onBackpressureLatest()
        .as(disposeOnDestroy(this))
        .subscribe(__ -> mapContainerViewModel.onMapDrag());
    getMapFragment()
        .getCameraMovedEvents()
        .onBackpressureLatest()
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::onCameraMove);
    getMapFragment()
        .getTileProviders()
        .as(disposeOnDestroy(this))
        .subscribe(mapContainerViewModel::queueTileProvider);

    polygonDrawingViewModel
        .getUnsavedMapLocationsOfInterest()
        .observe(this, mapContainerViewModel::setUnsavedMapLocationsOfInterest);
    locationOfInterestRepositionViewModel
        .getConfirmButtonClicks()
        .as(autoDisposable(this))
        .subscribe(this::showConfirmationDialog);
    locationOfInterestRepositionViewModel
        .getCancelButtonClicks()
        .as(autoDisposable(this))
        .subscribe(__ -> mapContainerViewModel.setMode(Mode.DEFAULT));
    mapContainerViewModel
        .getSelectMapTypeClicks()
        .as(autoDisposable(this))
        .subscribe(__ -> showMapTypeSelectorDialog());
    mapContainerViewModel
        .getZoomThresholdCrossed()
        .as(autoDisposable(this))
        .subscribe(__ -> onZoomThresholdCrossed());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = MapContainerFragBinding.inflate(inflater, container, false);
    binding.setViewModel(mapContainerViewModel);
    binding.setHomeScreenViewModel(homeScreenViewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    disableAddLocationOfInterestBtn();
  }

  @Override
  protected void onMapReady(MapFragment map) {
    Timber.d("MapAdapter ready. Updating subscriptions");

    // Custom views rely on the same instance of MapFragment. That couldn't be injected via Dagger.
    // Hence, initializing them here instead of inflating in layout.
    attachCustomViews(map);

    mapContainerViewModel.setLocationLockEnabled(true);
    polygonDrawingViewModel.setLocationLockEnabled(true);

    // Observe events emitted by the ViewModel.
    mapContainerViewModel.getMapLocationsOfInterest().observe(this, map::setMapLocationsOfInterest);
    mapContainerViewModel
        .getLocationLockState()
        .observe(this, state -> onLocationLockStateChange(state, map));
    mapContainerViewModel
        .getCameraUpdateRequests()
        .observe(this, update -> update.ifUnhandled(data -> onCameraUpdate(data, map)));
    mapContainerViewModel.getSurveyLoadingState().observe(this, this::onSurveyChange);
    homeScreenViewModel
        .getBottomSheetState()
        .observe(this, state -> onBottomSheetStateChange(state, map));
    mapContainerViewModel.getMbtilesFilePaths().observe(this, map::addLocalTileOverlays);

    // TODO: Do this the RxJava way
    CameraPosition cameraPosition = mapContainerViewModel.getCameraPosition().getValue();
    if (cameraPosition != null) {
      map.moveCamera(cameraPosition.getTarget(), cameraPosition.getZoomLevel());
    }
    mapsRepository.observableMapType().observe(this, map::setMapType);
  }

  private void attachCustomViews(MapFragment map) {
    LocationOfInterestRepositionView repositionView =
        new LocationOfInterestRepositionView(getContext(), map);
    mapContainerViewModel
        .getMoveLocationOfInterestVisibility()
        .observe(this, repositionView::setVisibility);
    binding.mapOverlay.addView(repositionView);

    PolygonDrawingView polygonDrawingView = new PolygonDrawingView(getContext(), map);
    mapContainerViewModel
        .getAddPolygonVisibility()
        .observe(this, polygonDrawingView::setVisibility);
    binding.mapOverlay.addView(polygonDrawingView);
  }

  /** Opens a dialog for selecting a [MapType] for the basemap layer. */
  private void showMapTypeSelectorDialog() {
    MapType[] types = stream(getMapFragment().getAvailableMapTypes()).toArray(MapType[]::new);
    findNavController(this)
        .navigate(
            HomeScreenFragmentDirections.actionHomeScreenFragmentToMapTypeDialogFragment(types));
  }

  private void showConfirmationDialog(Point point) {
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.move_point_confirmation)
        .setPositiveButton(android.R.string.ok, (dialog, which) -> moveToNewPosition(point))
        .setNegativeButton(
            android.R.string.cancel, (dialog, which) -> mapContainerViewModel.setMode(Mode.DEFAULT))
        .setCancelable(true)
        .create()
        .show();
  }

  private void moveToNewPosition(Point point) {
    Optional<LocationOfInterest> locationOfInterest =
        mapContainerViewModel.getReposLocationOfInterest();
    if (locationOfInterest.isEmpty()) {
      Timber.e("Move point failed: No locationOfInterest selected");
      return;
    }
    if (locationOfInterest.get().getGeometry().getType() != GeometryType.POINT) {
      Timber.e("Only point locations of interest can be moved");
      return;
    }
    LocationOfInterest<Point> newPointOfInterest =
        ((LocationOfInterest<Point>) locationOfInterest.get())
            .toBuilder().setGeometry(point).build();
    homeScreenViewModel.updateLocationOfInterest(newPointOfInterest);
  }

  private void onBottomSheetStateChange(BottomSheetState state, MapFragment map) {
    mapContainerViewModel.setSelectedLocationOfInterest(state.getLocationOfInterest());
    switch (state.getVisibility()) {
      case VISIBLE:
        map.disableGestures();
        // TODO(#358): Once polygon drawing is implemented, pan & zoom to polygon when
        // selected. This will involve calculating centroid and possibly zoom level based on
        // vertices.
        state
            .getLocationOfInterest()
            .filter(LocationOfInterest::isPoint)
            .map(x -> (LocationOfInterest<Point>) x)
            .ifPresent(
                pointOfInterest -> {
                  mapContainerViewModel.panAndZoomCamera(pointOfInterest.getGeometry());
                });
        break;
      case HIDDEN:
        map.enableGestures();
        break;
      default:
        Timber.e("Unhandled visibility: %s", state.getVisibility());
        break;
    }
  }

  private void onSurveyChange(Loadable<Survey> survey) {
    if (survey.isLoaded()) {
      enableAddLocationOfInterestBtn();
    } else {
      disableAddLocationOfInterestBtn();
    }
  }

  private void enableAddLocationOfInterestBtn() {
    mapContainerViewModel.setLocationOfInterestButtonBackgroundTint(R.color.colorMapAccent);
  }

  private void disableAddLocationOfInterestBtn() {
    // NOTE: We don't call addLocationOfInterestBtn.setEnabled(false) here since calling it before
    // the fab is
    // shown corrupts its padding when used with useCompatPadding="true".
    mapContainerViewModel.setLocationOfInterestButtonBackgroundTint(R.color.colorGrey500);
  }

  private void onLocationLockStateChange(BooleanOrError result, MapFragment map) {
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

  private void onCameraUpdate(MapContainerViewModel.CameraUpdate update, MapFragment map) {
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

  private void onZoomThresholdCrossed() {
    Timber.v("Refresh markers after zoom threshold crossed");

    getMapFragment().refreshMarkerIcons();
  }

  @Override
  public void onDestroy() {
    mapContainerViewModel.closeProviders();
    super.onDestroy();
  }
}
