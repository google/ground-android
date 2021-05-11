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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.android.gnd.util.ImmutableSetCollector.toImmutableSet;
import static java8.util.stream.StreamSupport.stream;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gnd.R;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.basemap.tile.TileSource;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.GeoJsonFeature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.OfflineBaseMapRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.rx.Action;
import com.google.android.gnd.rx.BooleanOrError;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.map.CameraPosition;
import com.google.android.gnd.ui.map.MapFeature;
import com.google.android.gnd.ui.map.MapGeoJson;
import com.google.android.gnd.ui.map.MapPin;
import com.google.android.gnd.ui.map.tileprovider.CloseableTileProvider;
import com.google.android.gnd.ui.map.tileprovider.LocalTileProvider;
import com.google.common.collect.ImmutableSet;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

@SharedViewModel
public class MapContainerViewModel extends AbstractViewModel {

  // A note on Zoom levels: The higher the number the more zoomed in the map will be.
  // 0.0f is fully zoomed out.
  private static final float DEFAULT_FEATURE_ZOOM_LEVEL = 20.0f;
  private static final float DEFAULT_MAP_ZOOM_LEVEL = 0.0f;
  private static final Point DEFAULT_MAP_POINT =
      Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build();

  private final LiveData<Loadable<Project>> projectLoadingState;
  private final LiveData<ImmutableSet<MapFeature>> mapFeatures;
  private final LiveData<BooleanOrError> locationLockState;
  private final LiveData<Event<CameraUpdate>> cameraUpdateRequests;

  @Hot(replays = true)
  private final MutableLiveData<CameraPosition> cameraPosition =
      new MutableLiveData<>(new CameraPosition(DEFAULT_MAP_POINT, DEFAULT_MAP_ZOOM_LEVEL));

  private final LocationManager locationManager;
  private final FeatureRepository featureRepository;

  @Hot private final Subject<Boolean> locationLockChangeRequests = PublishSubject.create();
  @Hot private final Subject<CameraUpdate> cameraUpdateSubject = PublishSubject.create();

  @Hot(replays = true)
  private final MutableLiveData<Integer> mapControlsVisibility = new MutableLiveData<>(VISIBLE);

  @Hot(replays = true)
  private final MutableLiveData<Integer> moveFeaturesVisibility = new MutableLiveData<>(GONE);

  @Hot(replays = true)
  private final MutableLiveData<Action> selectMapTypeClicks = new MutableLiveData<>();

  private final LiveData<ImmutableSet<String>> mbtilesFilePaths;
  private final LiveData<Integer> iconTint;
  private final List<LocalTileProvider> tileProviders = new ArrayList<>();

  // Feature currently selected for repositioning
  private Optional<Feature> selectedFeature = Optional.empty();

  @Inject
  MapContainerViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      LocationManager locationManager,
      OfflineBaseMapRepository offlineBaseMapRepository) {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    this.featureRepository = featureRepository;
    this.locationManager = locationManager;

    Flowable<BooleanOrError> locationLockStateFlowable = createLocationLockStateFlowable().share();
    this.locationLockState =
        LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable.startWith(BooleanOrError.falseValue()));
    this.iconTint =
        LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable
                .map(locked -> locked.isTrue() ? R.color.colorMapBlue : R.color.colorGrey800)
                .startWith(R.color.colorGrey800));
    this.cameraUpdateRequests =
        LiveDataReactiveStreams.fromPublisher(
            createCameraUpdateFlowable(locationLockStateFlowable));
    this.projectLoadingState =
        LiveDataReactiveStreams.fromPublisher(projectRepository.getProjectLoadingState());
    // TODO: Clear feature markers when project is deactivated.
    // TODO: Since we depend on project stream from repo anyway, this transformation can be moved
    // into the repo?
    this.mapFeatures =
        LiveDataReactiveStreams.fromPublisher(
            projectRepository
                .getActiveProject()
                .switchMap(this::getFeaturesStream)
                .map(MapContainerViewModel::toMapFeatures));
    this.mbtilesFilePaths =
        LiveDataReactiveStreams.fromPublisher(
            offlineBaseMapRepository
                .getDownloadedTileSourcesOnceAndStream()
                .map(set -> stream(set).map(TileSource::getPath).collect(toImmutableSet())));
  }

  private static ImmutableSet<MapFeature> toMapFeatures(ImmutableSet<Feature> features) {
    ImmutableSet<MapFeature> mapPins =
        stream(features)
            .filter(Feature::isPoint)
            .map(PointFeature.class::cast)
            .map(MapContainerViewModel::toMapPin)
            .collect(toImmutableSet());

    // TODO: Add support for polylines and polygons similar to mapPins

    ImmutableSet<MapFeature> mapPolygons =
        stream(features)
            .filter(Feature::isGeoJson)
            .map(GeoJsonFeature.class::cast)
            .map(MapContainerViewModel::toMapGeoJson)
            .collect(toImmutableSet());

    return ImmutableSet.<MapFeature>builder().addAll(mapPins).addAll(mapPolygons).build();
  }

  private static MapFeature toMapPin(PointFeature feature) {
    return MapPin.newBuilder()
        .setId(feature.getId())
        .setPosition(feature.getPoint())
        .setStyle(feature.getLayer().getDefaultStyle())
        .setFeature(feature)
        .build();
  }

  private static MapGeoJson toMapGeoJson(GeoJsonFeature feature) {
    JSONObject jsonObject;
    try {
      jsonObject = new JSONObject(feature.getGeoJsonString());
    } catch (JSONException e) {
      Timber.e(e);
      jsonObject = new JSONObject();
    }

    return MapGeoJson.newBuilder()
        .setId(feature.getId())
        .setGeoJson(jsonObject)
        .setStyle(feature.getLayer().getDefaultStyle())
        .build();
  }

  public LiveData<Action> getSelectMapTypeClicks() {
    return selectMapTypeClicks;
  }

  private Flowable<Event<CameraUpdate>> createCameraUpdateFlowable(
      Flowable<BooleanOrError> locationLockStateFlowable) {
    return cameraUpdateSubject
        .toFlowable(BackpressureStrategy.LATEST)
        .mergeWith(
            locationLockStateFlowable.switchMap(this::createLocationLockCameraUpdateFlowable))
        .map(Event::create);
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
    selectMapTypeClicks.postValue(Action.create());
  }

  public LiveData<Loadable<Project>> getProjectLoadingState() {
    return projectLoadingState;
  }

  public LiveData<ImmutableSet<MapFeature>> getMapFeatures() {
    return mapFeatures;
  }

  public LiveData<ImmutableSet<String>> getMbtilesFilePaths() {
    return mbtilesFilePaths;
  }

  LiveData<Event<CameraUpdate>> getCameraUpdateRequests() {
    return cameraUpdateRequests;
  }

  public LiveData<CameraPosition> getCameraPosition() {
    Timber.d("Current position is %s", cameraPosition.getValue().toString());
    return cameraPosition;
  }

  public LiveData<BooleanOrError> getLocationLockState() {
    return locationLockState;
  }

  public LiveData<Integer> getIconTint() {
    return iconTint;
  }

  private boolean isLocationLockEnabled() {
    return locationLockState.getValue().isTrue();
  }

  public void onCameraMove(CameraPosition newCameraPosition) {
    Timber.d("Setting position to %s", newCameraPosition.toString());
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

  // TODO(#691): Create our own wrapper/interface for MbTiles providers.
  public void queueTileProvider(LocalTileProvider tileProvider) {
    this.tileProviders.add(tileProvider);
  }

  public void closeProviders() {
    stream(tileProviders).forEach(provider -> {
      try {
        provider.close();
      } catch (IOException e) {
        Timber.e(e);
      }
    });
  }

  public void setViewMode(Mode viewMode) {
    mapControlsVisibility.setValue(viewMode == Mode.DEFAULT ? VISIBLE : GONE);
    moveFeaturesVisibility.setValue(viewMode == Mode.REPOSITION ? VISIBLE : GONE);
  }

  public LiveData<Integer> getMapControlsVisibility() {
    return mapControlsVisibility;
  }

  public LiveData<Integer> getMoveFeatureVisibility() {
    return moveFeaturesVisibility;
  }

  public Optional<Feature> getSelectedFeature() {
    return selectedFeature;
  }

  public void setSelectedFeature(Optional<Feature> selectedFeature) {
    this.selectedFeature = selectedFeature;
  }

  public enum Mode {
    DEFAULT,
    REPOSITION
  }

  static class CameraUpdate {

    private final Point center;
    private final Optional<Float> minZoomLevel;

    public CameraUpdate(Point center, Optional<Float> minZoomLevel) {
      this.center = center;
      this.minZoomLevel = minZoomLevel;
    }

    private static CameraUpdate pan(Point center) {
      return new CameraUpdate(center, Optional.empty());
    }

    private static CameraUpdate panAndZoom(Point center) {
      return new CameraUpdate(center, Optional.of(DEFAULT_FEATURE_ZOOM_LEVEL));
    }

    public Point getCenter() {
      return center;
    }

    public Optional<Float> getMinZoomLevel() {
      return minZoomLevel;
    }

    @NonNull
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
