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
import static android.view.View.INVISIBLE;
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
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.layer.Layer;
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
import com.google.android.gnd.ui.map.MapPolygon;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.ArrayList;
import java.util.Arrays;
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

  /** Polyline drawn by the user but not yet saved as polygon. */
  @Hot
  private final PublishProcessor<ImmutableList<Point>> drawnPolylineVertices =
      PublishProcessor.create();

  @Hot(replays = true)
  private final MutableLiveData<Integer> mapControlsVisibility = new MutableLiveData<>(VISIBLE);

  @Hot(replays = true)
  private final MutableLiveData<Integer> moveFeaturesVisibility = new MutableLiveData<>(GONE);

  @Hot(replays = true)
  private final MutableLiveData<Integer> polygonDrawingCompleted = new MutableLiveData<>(INVISIBLE);

  @Hot(replays = true)
  private final MutableLiveData<Integer> addPolygonPoints = new MutableLiveData<>(INVISIBLE);

  @Hot(replays = true)
  private final MutableLiveData<Action> selectMapTypeClicks = new MutableLiveData<>();

  @Hot(replays = true)
  private final MutableLiveData<Integer> addPolygonVisibility = new MutableLiveData<>(GONE);

  private final LiveData<ImmutableSet<String>> mbtilesFilePaths;
  private final LiveData<Integer> iconTint;
  private final List<MapBoxOfflineTileProvider> tileProviders = new ArrayList<>();

  // Feature currently selected for repositioning
  private Optional<Feature> selectedFeature = Optional.empty();

  private Optional<Layer> selectedLayer = Optional.empty();

  private Optional<Project> selectedProject = Optional.empty();

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
    // TODO: Need to update the UI as new points for polygon are added.
    // Features that are persisted to the local and remote dbs.
    Flowable<ImmutableSet<MapFeature>> persistentFeatures =
        projectRepository
            .getActiveProject()
            .switchMap(this::getFeaturesStream)
            .map(MapContainerViewModel::toMapFeatures);
    // Features not persisted to the db, but rather overlaid on the map due to some user
    // interaction (i.e., in progress polygon drawing flow).
    Flowable<ImmutableSet<MapFeature>> transientFeatures =
        drawnPolylineVertices.map(
            vertices ->
                ImmutableSet.of(
                    toMapPolygon(
                        featureRepository.newPolygonFeature(
                            selectedProject.get(), selectedLayer.get(), vertices))));
    this.mapFeatures = LiveDataReactiveStreams.fromPublisher(
        Flowable.combineLatest(Arrays.asList(
            persistentFeatures.startWith(ImmutableSet.<MapFeature>of()),
            transientFeatures.startWith(ImmutableSet.<MapFeature>of())),
            MapContainerViewModel::concatFeatureSets));
    this.mbtilesFilePaths =
        LiveDataReactiveStreams.fromPublisher(
            offlineBaseMapRepository
                .getDownloadedTileSourcesOnceAndStream()
                .map(set -> stream(set).map(TileSource::getPath).collect(toImmutableSet())));
  }

  private static ImmutableSet<MapFeature> concatFeatureSets(
      Object[] objects) {
    ImmutableSet<MapFeature> combinedFeatureSet = ImmutableSet.<MapFeature>builder().build();
    for (Object obj : objects) {
      if (obj instanceof ImmutableSet) {
        ImmutableSet<MapFeature> a = (ImmutableSet<MapFeature>) obj;
        combinedFeatureSet = ImmutableSet.<MapFeature>builder()
            .addAll(a).addAll(combinedFeatureSet).build();
      } else {
        Timber.d("Object is not of ImmutableSet class");
      }
    }
    return combinedFeatureSet;
  }

  private static ImmutableSet<MapFeature> toMapFeatures(ImmutableSet<Feature> features) {
    ImmutableSet<MapFeature> mapPins =
        stream(features)
            .filter(Feature::isPoint)
            .map(PointFeature.class::cast)
            .map(MapContainerViewModel::toMapPin)
            .collect(toImmutableSet());

    ImmutableSet<MapFeature> mapGeoJson =
        stream(features)
            .filter(Feature::isGeoJson)
            .map(GeoJsonFeature.class::cast)
            .map(MapContainerViewModel::toMapGeoJson)
            .collect(toImmutableSet());

    ImmutableSet<MapFeature> mapPolygons =
        stream(features)
            .filter(Feature::isPolygon)
            .map(PolygonFeature.class::cast)
            .map(MapContainerViewModel::toMapPolygon)
            .collect(toImmutableSet());

    return ImmutableSet.<MapFeature>builder()
        .addAll(mapPins)
        .addAll(mapGeoJson)
        .addAll(mapPolygons)
        .build();
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

  public void updateDrawnPolygonFeature(ImmutableList<Point> vertices) {
    drawnPolylineVertices.onNext(vertices);
  }

  private static MapFeature toMapPolygon(PolygonFeature feature) {
    return MapPolygon.newBuilder()
        .setId(feature.getId())
        .setVertices(feature.getVertices())
        .setStyle(feature.getLayer().getDefaultStyle())
        .setFeature(feature)
        .build();
  }

  public LiveData<Action> getSelectMapTypeClicks() {
    return selectMapTypeClicks;
  }

  public Optional<Layer> getSelectedLayer() {
    return selectedLayer;
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
  public void queueTileProvider(MapBoxOfflineTileProvider tileProvider) {
    this.tileProviders.add(tileProvider);
  }

  public void closeProviders() {
    stream(tileProviders).forEach(MapBoxOfflineTileProvider::close);
  }

  public void setViewMode(Mode viewMode) {
    mapControlsVisibility.setValue(viewMode == Mode.DEFAULT ? VISIBLE : GONE);
    moveFeaturesVisibility.setValue(viewMode == Mode.REPOSITION ? VISIBLE : GONE);
    addPolygonVisibility.setValue(viewMode == Mode.ADD_POLYGON ? VISIBLE : GONE);
  }

  public void updatePolygonDrawing(PolygonDrawing polygonDrawing) {
    addPolygonPoints.setValue(polygonDrawing == PolygonDrawing.DEFAULT ? VISIBLE : GONE);
    polygonDrawingCompleted.setValue(polygonDrawing == PolygonDrawing.COMPLETED ? VISIBLE : GONE);
  }

  public LiveData<Integer> getMapControlsVisibility() {
    return mapControlsVisibility;
  }

  public LiveData<Integer> getAddPolygonVisibility() {
    return addPolygonVisibility;
  }

  public LiveData<Integer> getPolygonDrawingCompletedVisibility() {
    return polygonDrawingCompleted;
  }

  public LiveData<Integer> getMoveFeatureVisibility() {
    return moveFeaturesVisibility;
  }

  public Optional<Feature> getSelectedFeature() {
    return selectedFeature;
  }

  public Optional<Project> getSelectedProject() {
    return selectedProject;
  }

  public void setSelectedFeature(Optional<Feature> selectedFeature) {
    this.selectedFeature = selectedFeature;
  }

  public void setSelectedLayer(Layer selectedLayer) {
    this.selectedLayer = Optional.of(selectedLayer);
  }

  public void setSelectedProject(Project selectedProject) {
    this.selectedProject = Optional.of(selectedProject);
  }

  public enum Mode {
    DEFAULT,
    REPOSITION,
    ADD_POLYGON
  }

  public enum PolygonDrawing {
    DEFAULT,
    COMPLETED
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
