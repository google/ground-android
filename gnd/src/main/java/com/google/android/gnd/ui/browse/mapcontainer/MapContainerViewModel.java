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

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Log;
import com.google.android.gnd.repository.GndDataRepository;
import com.google.android.gnd.repository.ProjectState;
import com.google.android.gnd.rx.RxLiveData;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.ui.browse.AddPlaceDialogFragment.AddPlaceRequest;
import com.google.android.gnd.ui.map.MapMarker;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Point;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import java8.util.Optional;
import javax.inject.Inject;

public class MapContainerViewModel extends ViewModel {
  private static final String TAG = MapContainerViewModel.class.getSimpleName();
  private static final float DEFAULT_ZOOM_LEVEL = 14.0f;
  private final MutableLiveData<ProjectState> projectState;
  private final LiveData<ImmutableSet<Place>> places;
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
    this.projectState = new MutableLiveData<>();
    this.places =
      RxLiveData.fromFlowable(
        dataRepository
          .getProjectState()
          .doOnNext(projectState::postValue)
          .filter(ProjectState::isActivated)
          .switchMap(ProjectState::getPlaces));
  }

  //  private void updatePlaces(MarkerUpdate markerUpdate) {
  //    switch (markerUpdate.getType()) {
  //
  //      case CLEAR_ALL:
  //        places.setValue(ImmutableSet.of());
  //        break;
  //      case ADD_OR_UPDATE_MARKER:
  //        places.setValue(
  //        ImmutableSet.<Place>builder()
  //          .addAll(places.getValue())
  //          .add(markerUpdate.getPlace())
  //          .build());
  //        break;
  //      case REMOVE_MARKER:
  //        places.setValue(
  //          stream(places.getValue()).filter(p -> !p.getPlaceType(markerUpdate.getPlace())).
  //            );
  //        break;
  //    }
  //  }

  public LiveData<ProjectState> getProjectState() {
    return projectState;
  }

  public LiveData<ImmutableSet<Place>> getPlaces() {
    return places;
  }

  LiveData<CameraUpdate> getCameraUpdates() {
    return cameraUpdates;
  }

  public MutableLiveData<LocationLockStatus> getLocationLockStatus() {
    return locationLockStatus;
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
