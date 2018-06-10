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

package com.google.android.gnd.ui.mapcontainer;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.graphics.Color;
import android.util.Log;
import com.google.android.gnd.repository.GndDataRepository;
import com.google.android.gnd.repository.ProjectState;
import com.google.android.gnd.rx.RxLiveData;
import com.google.android.gnd.service.DatastoreEvent;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.ui.AddPlaceDialogFragment.AddPlaceRequest;
import com.google.android.gnd.ui.map.MapMarker;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.PlaceType;
import com.google.android.gnd.vo.Point;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import java8.util.Optional;
import javax.inject.Inject;

public class MapContainerViewModel extends ViewModel {
  private static final String TAG = MapContainerViewModel.class.getSimpleName();
  private static final float DEFAULT_ZOOM_LEVEL = 14.0f;
  private final MutableLiveData<ProjectState> projectStates;
  private final LiveData<MarkerUpdate> markerUpdates;
  private final MutableLiveData<LocationLockStatus> locationLockStatus;
  private final MutableLiveData<CameraUpdate> cameraUpdates;
  private final LocationManager locationManager;
  // TODO: Use pure Rx rather than disposable.
  private Disposable locationUpdateSubscription;

  @Inject
  MapContainerViewModel(GndDataRepository dataRepository, LocationManager locationManager) {
    this.locationManager = locationManager;
    this.locationLockStatus = new MutableLiveData<>();
    locationLockStatus.setValue(LocationLockStatus.disabled());
    this.cameraUpdates = new MutableLiveData<>();
    this.projectStates = new MutableLiveData<>();
    this.markerUpdates =
      RxLiveData.fromFlowable(
        dataRepository
          .getProjectState()
          .doOnNext(projectStates::postValue)
          .filter(ProjectState::isActivated)
          .switchMap(MapContainerViewModel::toMarkerUpdateFlowable));
  }

  private static Flowable<MarkerUpdate> toMarkerUpdateFlowable(ProjectState projectState) {
    return projectState
      .getPlaces()
      // Convert each place update into a marker update.
      .map(placeData -> toMarkerUpdate(placeData))
      // Drop updates that are invalid or do not apply.
      .filter(MarkerUpdate::isValid)
      // Clear all markers when active project changes.
      .startWith(MarkerUpdate.clearAll());
  }

  public MutableLiveData<ProjectState> projectStates() {
    return projectStates;
  }

  LiveData<MarkerUpdate> mapMarkers() {
    return markerUpdates;
  }

  LiveData<CameraUpdate> cameraUpdates() {
    return cameraUpdates;
  }

  public MutableLiveData<LocationLockStatus> locationLockStatus() {
    return locationLockStatus;
  }

  private static MarkerUpdate toMarkerUpdate(DatastoreEvent<Place> placeData) {
    switch (placeData.getType()) {
      case ENTITY_LOADED:
      case ENTITY_MODIFIED:
        return placeData
          .getEntity()
          .map(
            place ->
              MarkerUpdate.addOrUpdatePlace(
                place, // TODO: Remove Place from MarkerUpdate.
                place.getPlaceType().getIconId(),
                getIconColor(place.getPlaceType()),
                placeData.hasPendingWrites()))
          .orElse(MarkerUpdate.invalid());
      case ENTITY_REMOVED:
        return MarkerUpdate.remove(placeData.getId());
    }
    return MarkerUpdate.invalid();
  }

  private static int getIconColor(PlaceType placeType) {
    // TODO: Return default color if invalid.
    // TODO: Refactor into model.
    return Color.parseColor(placeType.getIconColor());
  }

  public void onLocationLockClick() {
    if (isLocationLockEnabled()) {
      disableLocationLock();
    } else {
      enableLocationLock();
    }
  }

  private boolean isLocationLockEnabled() {
    return locationLockStatus.getValue().isEnabled();
  }

  @SuppressLint("CheckResult")
  private void enableLocationLock() {
    locationManager
      .enableLocationUpdates()
      .subscribe(this::onEnableLocationLockSuccess, this::onLocationFailure);
  }

  private void onEnableLocationLockSuccess() {
    locationLockStatus.setValue(LocationLockStatus.enabled());
    Flowable<Point> locationUpdates = locationManager.locationUpdates();
    // TODO: Use pure Rx rather than disposable.
    locationUpdateSubscription =
      locationUpdates
        .take(1)
        .map(CameraUpdate::panAndZoom)
        .concatWith(locationUpdates.map(CameraUpdate::pan))
        .subscribe(cameraUpdates::setValue);
  }

  private void onLocationFailure(Throwable t) {
    locationLockStatus.setValue(LocationLockStatus.error(t));
  }

  @SuppressLint("CheckResult")
  private void disableLocationLock() {
    Log.d(TAG, "Disabling location lock");
    locationManager.disableLocationUpdates().subscribe(this::onDisableLocationLockSuccess);
  }

  private void onDisableLocationLockSuccess() {
    locationLockStatus.setValue(LocationLockStatus.disabled());
    if (locationUpdateSubscription != null) {
      locationUpdateSubscription.dispose();
      locationUpdateSubscription = null;
    }
  }

  public void onAddPlace(AddPlaceRequest addPlaceRequest) {
    // TODO: Transition to add place view.
  }

  public void onMapDrag(Point point) {
    if (isLocationLockEnabled()) {
      Log.d(TAG, "User dragged map. Disabling location lock");
      disableLocationLock();
    }
  }

  public void onMarkerClick(MapMarker mapMarker) {
    cameraUpdates.setValue(CameraUpdate.panAndZoom(mapMarker.getPosition()));
  }

  public static class LocationLockStatus {
    private boolean enabled;
    private Throwable error;

    private LocationLockStatus(boolean enabled) {
      this.enabled = enabled;
    }

    private LocationLockStatus(Throwable error) {
      this.error = error;
    }

    private static LocationLockStatus enabled() {
      return new LocationLockStatus(true);
    }

    private static LocationLockStatus disabled() {
      return new LocationLockStatus(false);
    }

    private static LocationLockStatus error(Throwable t) {
      return new LocationLockStatus(t);
    }

    public boolean isEnabled() {
      return enabled;
    }

    public boolean isError() {
      return error != null;
    }

    public Throwable getError() {
      return error;
    }
  }

  static class CameraUpdate {
    private Point center;
    private Optional<Float> minZoomLevel;

    public CameraUpdate(Point center, Optional<Float> minZoomLevel) {
      this.center = center;
      this.minZoomLevel = minZoomLevel;
    }

    public Point getCenter() {
      return center;
    }

    public Optional<Float> getMinZoomLevel() {
      return minZoomLevel;
    }

    private static CameraUpdate pan(Point center) {
      return new CameraUpdate(center, Optional.empty());
    }

    private static CameraUpdate panAndZoom(Point center) {
      return new CameraUpdate(center, Optional.of(DEFAULT_ZOOM_LEVEL));
    }

    @Override
    public String toString() {
      if (minZoomLevel.isPresent()) {
        return "Pan + zoom";
      } else {
        return "Pan";
      }
    }
  }
}
