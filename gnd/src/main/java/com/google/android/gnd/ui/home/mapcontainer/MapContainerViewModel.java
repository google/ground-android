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

import static com.google.android.gnd.util.ImmutableSetCollector.toImmutableSet;
import static java8.util.stream.StreamSupport.stream;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.basemap.tile.TileSource;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.OfflineBaseMapRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.rx.BooleanOrError;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.map.MapPin;
import com.google.common.collect.ImmutableSet;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@SharedViewModel
public class MapContainerViewModel extends AbstractViewModel {

  private static final float DEFAULT_ZOOM_LEVEL = 20.0f;
  private final LiveData<Loadable<Project>> activeProject;
  private final LiveData<ImmutableSet<MapPin>> mapPins;
  private final LiveData<BooleanOrError> locationLockState;
  private final LiveData<CameraUpdate> cameraUpdateRequests;
  private final MutableLiveData<Point> cameraPosition;
  private final LocationManager locationManager;
  private final FeatureRepository featureRepository;
  private final Subject<Boolean> locationLockChangeRequests;
  private final Subject<CameraUpdate> cameraUpdateSubject;
  private final MutableLiveData<Event<Nil>> showMapTypeSelectorRequests = new MutableLiveData<>();
  private final LiveData<ImmutableSet<String>> mbtilesFilePaths;
  // TODO: Create our own wrapper/interface for MbTiles providers
  // The impl we're using unfortunately requires calling a `close` method explicitly
  // to clean up provider resources; `close` however, is not defined by the `TileProvider`
  // interface, preventing us from treating providers generically.
  private final List<MapBoxOfflineTileProvider> tileProviders = new ArrayList<>();

  @Inject
  MapContainerViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      LocationManager locationManager,
      OfflineBaseMapRepository offlineBaseMapRepository) {
    this.featureRepository = featureRepository;
    this.locationManager = locationManager;
    this.locationLockChangeRequests = PublishSubject.create();
    this.cameraUpdateSubject = PublishSubject.create();

    Flowable<BooleanOrError> locationLockStateFlowable = createLocationLockStateFlowable().share();
    this.locationLockState =
        LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable.startWith(BooleanOrError.falseValue()));
    this.cameraUpdateRequests =
        LiveDataReactiveStreams.fromPublisher(
            createCameraUpdateFlowable(locationLockStateFlowable));
    this.cameraPosition = new MutableLiveData<>();
    this.activeProject =
        LiveDataReactiveStreams.fromPublisher(projectRepository.getActiveProjectOnceAndStream());
    // TODO: Clear feature markers when project is deactivated.
    // TODO: Since we depend on project stream from repo anyway, this transformation can be moved
    // into the repo?
    this.mapPins =
        LiveDataReactiveStreams.fromPublisher(
            projectRepository
                .getActiveProjectOnceAndStream()
                .map(Loadable::value)
                .switchMap(this::getFeaturesStream)
                .map(MapContainerViewModel::toMapPins));
    this.mbtilesFilePaths =
        LiveDataReactiveStreams.fromPublisher(
            offlineBaseMapRepository
                .getDownloadedTileSourcesOnceAndStream()
                .map(set -> stream(set).map(TileSource::getPath).collect(toImmutableSet())));
  }

  private Flowable<CameraUpdate> createCameraUpdateFlowable(
      Flowable<BooleanOrError> locationLockStateFlowable) {
    return cameraUpdateSubject
        .toFlowable(BackpressureStrategy.LATEST)
        .mergeWith(
            locationLockStateFlowable.switchMap(
                state -> createLocationLockCameraUpdateFlowable(state)));
  }

  private Flowable<CameraUpdate> createLocationLockCameraUpdateFlowable(BooleanOrError lockState) {
    if (!lockState.isTrue()) {
      return Flowable.empty();
    }
    // The first update pans and zooms the camera to the appropriate zoom level; subsequent ones
    // only pan the map.
    Flowable<Point> locationUpdates = locationManager.getLocationUpdates();
    return locationUpdates
        .take(1)
        .map(CameraUpdate::panAndZoom)
        .concatWith(locationUpdates.map(CameraUpdate::pan).skip(1));
  }

  private Flowable<BooleanOrError> createLocationLockStateFlowable() {
    return locationLockChangeRequests
        .throttleFirst(200, TimeUnit.MILLISECONDS)
        .switchMapSingle(
            enabled ->
                enabled
                    ? this.locationManager.enableLocationUpdates()
                    : this.locationManager.disableLocationUpdates())
        .toFlowable(BackpressureStrategy.LATEST);
  }

  private Flowable<ImmutableSet<Feature>> getFeaturesStream(Optional<Project> activeProject) {
    // Emit empty set in separate stream to force unsubscribe from Feature updates and update
    // subscribers.
    return activeProject
        .map(featureRepository::getFeaturesOnceAndStream)
        .orElse(Flowable.just(ImmutableSet.of()));
  }

  public void onMapTypeButtonClicked() {
    showMapTypeSelectorRequests.setValue(Event.create(Nil.NIL));
  }

  private static ImmutableSet<MapPin> toMapPins(ImmutableSet<Feature> features) {
    return stream(features).map(MapContainerViewModel::toMapPin).collect(toImmutableSet());
  }

  private static MapPin toMapPin(Feature feature) {
    return MapPin.newBuilder()
        .setId(feature.getId())
        .setPosition(feature.getPoint())
        .setStyle(feature.getLayer().getDefaultStyle())
        .setFeature(feature)
        .build();
  }

  public LiveData<Loadable<Project>> getActiveProject() {
    return activeProject;
  }

  public LiveData<ImmutableSet<MapPin>> getMapPins() {
    return mapPins;
  }

  public LiveData<ImmutableSet<String>> getMbtilesFilePaths() {
    return mbtilesFilePaths;
  }

  LiveData<CameraUpdate> getCameraUpdateRequests() {
    return cameraUpdateRequests;
  }

  public LiveData<Point> getCameraPosition() {
    return cameraPosition;
  }

  public LiveData<BooleanOrError> getLocationLockState() {
    return locationLockState;
  }

  private boolean isLocationLockEnabled() {
    return locationLockState.getValue().isTrue();
  }

  public void onCameraMove(Point newCameraPosition) {
    this.cameraPosition.setValue(newCameraPosition);
  }

  public void onMapDrag(Point newCameraPosition) {
    if (isLocationLockEnabled()) {
      Timber.d("User dragged map. Disabling location lock");
      locationLockChangeRequests.onNext(false);
    }
  }

  public void onMarkerClick(MapPin pin) {
    panAndZoomCamera(pin.getPosition());
  }

  public void panAndZoomCamera(Point position) {
    cameraUpdateSubject.onNext(CameraUpdate.panAndZoom(position));
  }

  public void onLocationLockClick() {
    locationLockChangeRequests.onNext(!isLocationLockEnabled());
  }

  LiveData<Event<Nil>> getShowMapTypeSelectorRequests() {
    return showMapTypeSelectorRequests;
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

  public void queueTileProvider(MapBoxOfflineTileProvider tileProvider) {
    this.tileProviders.add(tileProvider);
  }

  public void closeProviders() {
    stream(tileProviders).forEach(MapBoxOfflineTileProvider::close);
  }
}
