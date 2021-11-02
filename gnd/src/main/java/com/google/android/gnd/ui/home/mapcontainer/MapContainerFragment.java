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

import android.app.ActionBar.LayoutParams;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.PopupWindow;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.MapContainerFragBinding;
import com.google.android.gnd.databinding.MapLayersPopupWindowBinding;
import com.google.android.gnd.databinding.MapTypeItemBinding;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.repository.MapsRepository;
import com.google.android.gnd.rx.BooleanOrError;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import com.google.android.gnd.system.SettingsManager.SettingsChangeRequestCanceled;
import com.google.android.gnd.ui.common.AbstractMapViewerFragment;
import com.google.android.gnd.ui.home.BottomSheetState;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel.Mode;
import com.google.android.gnd.ui.map.CameraPosition;
import com.google.android.gnd.ui.map.MapFragment;
import com.google.android.gnd.ui.map.MapType;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import java8.util.Optional;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint
public class MapContainerFragment extends AbstractMapViewerFragment {

  @Inject MapsRepository mapsRepository;
  PolygonDrawingViewModel polygonDrawingViewModel;
  private MapContainerViewModel mapContainerViewModel;
  private HomeScreenViewModel homeScreenViewModel;
  private MapContainerFragBinding binding;
  @Nullable private PopupWindow layersPopupWindow;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mapContainerViewModel = getViewModel(MapContainerViewModel.class);
    homeScreenViewModel = getViewModel(HomeScreenViewModel.class);
    FeatureRepositionViewModel featureRepositionViewModel =
        getViewModel(FeatureRepositionViewModel.class);
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
        .getFeatureClicks()
        .as(disposeOnDestroy(this))
        .subscribe(homeScreenViewModel::onFeatureClick);
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
        .getDefaultMapMode()
        .as(autoDisposable(this))
        .subscribe(__ -> setDefaultMode());
    polygonDrawingViewModel
        .getPolygonFeature()
        .observe(this, feature -> mapContainerViewModel.updateDrawnPolygonFeature(feature));
    featureRepositionViewModel
        .getConfirmButtonClicks()
        .as(autoDisposable(this))
        .subscribe(this::showConfirmationDialog);
    featureRepositionViewModel
        .getCancelButtonClicks()
        .as(autoDisposable(this))
        .subscribe(__ -> setDefaultMode());
    mapContainerViewModel
        .getSelectMapTypeClicks()
        .as(autoDisposable(this))
        .subscribe(__ -> showMapLayersPopupWindow());
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
    disableAddFeatureBtn();
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
    mapContainerViewModel.getMbtilesFilePaths().observe(this, map::addLocalTileOverlays);

    // TODO: Do this the RxJava way
    CameraPosition cameraPosition = mapContainerViewModel.getCameraPosition().getValue();
    if (cameraPosition != null) {
      map.moveCamera(cameraPosition.getTarget(), cameraPosition.getZoomLevel());
    }
    map.setMapType(mapsRepository.getSavedMapType());
  }

  private void attachCustomViews(MapFragment map) {
    FeatureRepositionView repositionView = new FeatureRepositionView(getContext(), map);
    mapContainerViewModel.getMoveFeatureVisibility().observe(this, repositionView::setVisibility);
    binding.mapOverlay.addView(repositionView);

    PolygonDrawingView polygonDrawingView = new PolygonDrawingView(getContext(), map);
    mapContainerViewModel
        .getAddPolygonVisibility()
        .observe(this, polygonDrawingView::setVisibility);
    binding.mapOverlay.addView(polygonDrawingView);
  }

  private void showMapLayersPopupWindow() {
    if (layersPopupWindow == null) {
      layersPopupWindow = createLayersPopupWindow();
    }
    layersPopupWindow.showAsDropDown(binding.mapControls.mapTypeBtn);
  }

  private PopupWindow createLayersPopupWindow() {
    MapLayersPopupWindowBinding popupWindowBinding = MapLayersPopupWindowBinding
        .inflate(getLayoutInflater());
    PopupWindow popupWindow = new PopupWindow(
        popupWindowBinding.getRoot(),
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT,
        /* focusable= */ true);

    MapTypesAdapter mapTypeAdapter =
        new MapTypesAdapter(
            stream(getMapFragment().getAvailableMapTypes())
                .map(
                    mapType ->
                        new MapTypeItem(
                            mapType.getType(), getString(mapType.getLabelId()), getMapFragment()))
                .collect(toImmutableList()));
    popupWindowBinding.mapTypeListView.setAdapter(mapTypeAdapter);
    popupWindowBinding.mapTypeListView.setOnItemClickListener((adapterView, view, position, id) -> {
      int mapType = (int) mapTypeAdapter.getItem(position);
      getMapFragment().setMapType(mapType);
      mapsRepository.saveMapType(mapType);
      popupWindow.dismiss();
    });

    ArrayAdapter<String> dataLayerAdapter =
        new ArrayAdapter<>(requireActivity(), R.layout.data_layer_item, R.id.data_layer_text_view);
    popupWindowBinding.dataLayerListView.setAdapter(dataLayerAdapter);
    mapContainerViewModel
        .getLayers()
        .observe(
            this,
            layers -> {
              dataLayerAdapter.clear();
              dataLayerAdapter.addAll(
                  stream(layers).map(Layer::getName).collect(toImmutableList()));
            });

    return popupWindow;
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

  private void onBottomSheetStateChange(BottomSheetState state, MapFragment map) {
    mapContainerViewModel.setSelectedFeature(state.getFeature());
    switch (state.getVisibility()) {
      case VISIBLE:
        map.disableGestures();
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
        map.enableGestures();
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
