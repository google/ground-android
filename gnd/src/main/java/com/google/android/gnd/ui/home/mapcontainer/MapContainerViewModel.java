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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.rx.RxLiveData;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.ActivityScope;
import com.google.android.gnd.ui.map.MapMarker;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import java8.util.Optional;
import javax.inject.Inject;

@ActivityScope
public class MapContainerViewModel extends AbstractViewModel {

  private static final String TAG = MapContainerViewModel.class.getSimpleName();
  private static final float DEFAULT_ZOOM_LEVEL = 14.0f;
  private final LiveData<Resource<Project>> activeProject;
  private final LiveData<ImmutableSet<Place>> places;
  private final MutableLiveData<LocationLockStatus> locationLockStatus;
  private final MutableLiveData<CameraUpdate> cameraUpdates;
  private final MutableLiveData<Point> cameraPosition;
  private final LocationManager locationManager;
  private final DataRepository dataRepository;
  private Disposable locationUpdateSubscription;

  @Inject
  MapContainerViewModel(DataRepository dataRepository, LocationManager locationManager) {
    this.dataRepository = dataRepository;
    this.locationManager = locationManager;
    this.locationLockStatus = new MutableLiveData<>();
    locationLockStatus.setValue(LocationLockStatus.disabled());
    this.cameraUpdates = new MutableLiveData<>();
    this.cameraPosition = new MutableLiveData<>();
    this.activeProject = RxLiveData.fromFlowable(dataRepository.getActiveProject());
    // TODO: Clear place markers when project is deactivated.
    // TODO: Since we depend on project stream from repo anyway, this transformation can be moved
    // into the repo.
    this.places =
        RxLiveData.fromFlowable(
            dataRepository
              .getActiveProject()
              .map(Resource::getData)
              .switchMap(this::getPlacesStream));
  }

  private Flowable<ImmutableSet<Place>> getPlacesStream(Optional<Project> activeProject) {
    // Emit empty set in separate stream to force unsubscribe from Place updates and update
    // subscribers.
    return activeProject
      .map(dataRepository::getPlaceVectorStream)
      .orElse(Flowable.just(ImmutableSet.of()));
  }

  public LiveData<Resource<Project>> getActiveProject() {
    return activeProject;
  }

  public LiveData<ImmutableSet<Place>> getPlaces() {
    return places;
  }

  LiveData<CameraUpdate> getCameraUpdates() {
    return cameraUpdates;
  }

  public LiveData<Point> getCameraPosition() {
    return cameraPosition;
  }

  public LiveData<LocationLockStatus> getLocationLockStatus() {
    return locationLockStatus;
  }

  public boolean isLocationLockEnabled() {
    return locationLockStatus.getValue().isEnabled();
  }

  public void enableLocationLock() {
    disposeOnClear(
        locationManager
            .enableLocationUpdates()
            .doOnComplete(() -> locationLockStatus.setValue(LocationLockStatus.enabled()))
            .doOnComplete(() -> restartLocationUpdates())
            .doOnError(t -> locationLockStatus.setValue(LocationLockStatus.error(t)))
            .subscribe());
  }

  private void restartLocationUpdates() {
    disposeLocationUpdateSubscription();

    // Sometimes there is visible latency between when location update request succeeds and when
    // the first location update is received. Requesting the last know location is usually
    // immediate, so we request it first here to reduce perceived latency.
    // The first update pans and zooms the camera to the appropriate zoom level; subsequent ones
    // only pan the map.
    Flowable<Point> locations =
        locationManager
            .getLastLocation()
            .toFlowable()
            .concatWith(locationManager.getLocationUpdates());

    locationUpdateSubscription =
        locations
            .take(1)
            .map(CameraUpdate::panAndZoom)
            .concatWith(locations.map(CameraUpdate::pan).skip(1))
            .subscribe(cameraUpdates::setValue);

    Log.d(TAG, "Enable location lock succeeded");
  }

  public void disableLocationLock() {
    disposeOnClear(
        locationManager.disableLocationUpdates().subscribe(this::onLocationLockDisabled));
  }

  private void onLocationLockDisabled() {
    disposeLocationUpdateSubscription();
    locationLockStatus.setValue(LocationLockStatus.disabled());
  }

  public void onCameraMove(Point newCameraPosition) {
    this.cameraPosition.setValue(newCameraPosition);
  }

  public void onMapDrag(Point newCameraPosition) {
    if (isLocationLockEnabled()) {
      Log.d(TAG, "User dragged map. Disabling location lock");
      disableLocationLock();
    }
  }

  public void onMarkerClick(MapMarker mapMarker) {
    cameraUpdates.setValue(CameraUpdate.panAndZoom(mapMarker.getPosition()));
  }

  @Override
  protected void onCleared() {
    disposeLocationUpdateSubscription();
  }

  private synchronized void disposeLocationUpdateSubscription() {
    if (locationUpdateSubscription != null) {
      locationUpdateSubscription.dispose();
      locationUpdateSubscription = null;
    }
  }

  static class LocationLockStatus {

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
