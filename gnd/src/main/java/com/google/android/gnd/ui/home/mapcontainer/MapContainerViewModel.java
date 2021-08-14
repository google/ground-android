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

import android.content.res.Resources;
import android.location.Location;
import androidx.annotation.ColorRes;
import androidx.annotation.Dimension;
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
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.OfflineBaseMapRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.rx.BooleanOrError;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.map.CameraPosition;
import com.google.android.gnd.ui.map.MapFeature;
import com.google.android.gnd.ui.map.MapGeoJson;
import com.google.android.gnd.ui.map.MapPin;
import com.google.android.gnd.ui.map.MapPolygon;
import com.google.common.collect.ImmutableSet;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.ArrayList;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

@SharedViewModel
public class MapContainerViewModel extends AbstractViewModel {

  // Higher zoom levels means the map is more zoomed in. 0.0f is fully zoomed out.
  private static final float DEFAULT_FEATURE_ZOOM_LEVEL = 18.0f;
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

  private final Resources resources;
  private final ProjectRepository projectRepository;
  private final LocationManager locationManager;
  private final FeatureRepository featureRepository;

  @Hot private final Subject<Boolean> locationLockChangeRequests = PublishSubject.create();
  @Hot private final Subject<CameraUpdate> cameraUpdateSubject = PublishSubject.create();

  @Hot(replays = true)
  private final MutableLiveData<Integer> mapControlsVisibility = new MutableLiveData<>(VISIBLE);

  private final MutableLiveData<Boolean> completeButtonVisible = new MutableLiveData<>(false);

  private final MutableLiveData<Boolean> addPolygonVisible = new MutableLiveData<>(false);

  @Hot(replays = true)
  private final MutableLiveData<Integer> moveFeaturesVisibility = new MutableLiveData<>(GONE);

  @Hot(replays = true)
  private final MutableLiveData<Boolean> locationLockEnabled = new MutableLiveData<>();

  @Hot(replays = true)
  private final MutableLiveData<Integer> featureAddButtonBackgroundTint =
      new MutableLiveData<>(R.color.colorGrey500);

  private final LiveData<ImmutableSet<String>> mbtilesFilePaths;
  private final LiveData<Integer> iconTint;
  private final LiveData<Boolean> locationUpdatesEnabled;
  private final LiveData<String> locationAccuracy;
  private final List<MapBoxOfflineTileProvider> tileProviders = new ArrayList<>();
  private final @Dimension int defaultPolygonStrokeWidth;
  private final @Dimension int selectedPolygonStrokeWidth;
  /** The currently selected feature on the map. */
  private final BehaviorProcessor<Optional<Feature>> selectedFeature =
      BehaviorProcessor.createDefault(Optional.empty());
  /* UI Clicks */
  @Hot private final Subject<Nil> selectMapTypeClicks = PublishSubject.create();
  @Hot private final Subject<Point> addFeatureButtonClicks = PublishSubject.create();
  /** Feature selected for repositioning. */
  private Optional<Feature> reposFeature = Optional.empty();

  @Inject
  MapContainerViewModel(
      Resources resources,
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      LocationManager locationManager,
      OfflineBaseMapRepository offlineBaseMapRepository) {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    this.resources = resources;
    this.projectRepository = projectRepository;
    this.featureRepository = featureRepository;
    this.locationManager = locationManager;
    this.defaultPolygonStrokeWidth = (int) resources.getDimension(R.dimen.polyline_stroke_width);
    this.selectedPolygonStrokeWidth =
        (int) resources.getDimension(R.dimen.selected_polyline_stroke_width);
    Flowable<BooleanOrError> locationLockStateFlowable = createLocationLockStateFlowable().share();
    this.locationLockState =
        LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable.startWith(BooleanOrError.falseValue()));
    this.iconTint =
        LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable
                .map(locked -> locked.isTrue() ? R.color.colorMapBlue : R.color.colorGrey800)
                .startWith(R.color.colorGrey800));
    this.locationUpdatesEnabled =
        LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable.map(BooleanOrError::isTrue).startWith(false));
    this.locationAccuracy =
        LiveDataReactiveStreams.fromPublisher(
            createLocationAccuracyFlowable(locationLockStateFlowable));
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
            Flowable.combineLatest(
                    projectRepository
                        .getActiveProject()
                        .switchMap(this::getFeaturesStream)
                        .map(this::toMapFeatures),
                    selectedFeature,
                    this::updateSelectedFeature)
                .distinctUntilChanged());
    this.mbtilesFilePaths =
        LiveDataReactiveStreams.fromPublisher(
            offlineBaseMapRepository
                .getDownloadedTileSourcesOnceAndStream()
                .map(set -> stream(set).map(TileSource::getPath).collect(toImmutableSet())));
    disposeOnClear(projectRepository.getActiveProject().subscribe(this::onProjectChange));
  }

  private static MapFeature toMapPin(PointFeature feature) {
    return MapPin.newBuilder()
        .setId(feature.getId())
        .setPosition(feature.getPoint())
        .setStyle(feature.getLayer().getDefaultStyle())
        .setFeature(feature)
        .build();
  }

  private static MapFeature toMapPolygon(PolygonFeature feature) {
    return MapPolygon.newBuilder()
        .setId(feature.getId())
        .setVertices(feature.getVertices())
        .setStyle(feature.getLayer().getDefaultStyle())
        .setFeature(feature)
        .build();
  }

  private void onProjectChange(Optional<Project> project) {
    project
        .map(Project::getId)
        .flatMap(projectRepository::getLastCameraPosition)
        .ifPresent(this::panAndZoomCamera);
  }

  private ImmutableSet<MapFeature> updateSelectedFeature(
      ImmutableSet<MapFeature> features, Optional<Feature> selectedFeature) {
    Timber.v("Updating selected feature style");
    if (selectedFeature.isEmpty()) {
      return features;
    }
    ImmutableSet.Builder updatedFeatures = ImmutableSet.builder();
    String selectedFeatureId = selectedFeature.get().getId();
    for (MapFeature feature : features) {
      if (feature instanceof MapGeoJson) {
        MapGeoJson geoJsonFeature = (MapGeoJson) feature;
        String geoJsonFeatureId = geoJsonFeature.getFeature().getId();
        if (geoJsonFeatureId.equals(selectedFeatureId)) {
          Timber.v("Restyling selected GeoJSON feature " + selectedFeatureId);
          updatedFeatures.add(
              geoJsonFeature.toBuilder().setStrokeWidth(selectedPolygonStrokeWidth).build());
          continue;
        }
      }
      updatedFeatures.add(feature);
    }
    return updatedFeatures.build();
  }

  private ImmutableSet<MapFeature> toMapFeatures(ImmutableSet<Feature> features) {
    ImmutableSet<MapFeature> mapPins =
        stream(features)
            .filter(Feature::isPoint)
            .map(PointFeature.class::cast)
            .map(MapContainerViewModel::toMapPin)
            .collect(toImmutableSet());

    // TODO: Add support for polylines similar to mapPins.

    ImmutableSet<MapFeature> mapGeoJson =
        stream(features)
            .filter(Feature::isGeoJson)
            .map(GeoJsonFeature.class::cast)
            .map(this::toMapGeoJson)
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

  private MapGeoJson toMapGeoJson(GeoJsonFeature feature) {
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
        .setStrokeWidth(defaultPolygonStrokeWidth)
        .setFeature(feature)
        .build();
  }

  private Flowable<String> createLocationAccuracyFlowable(Flowable<BooleanOrError> lockState) {
    return lockState.switchMap(
        booleanOrError ->
            booleanOrError.isTrue()
                ? locationManager
                    .getLocationUpdates()
                    .map(Location::getAccuracy)
                    .map(accuracy -> resources.getString(R.string.location_accuracy, accuracy))
                : Flowable.empty());
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
    Flowable<Point> locationUpdates =
        locationManager.getLocationUpdates().map(LocationManager::toPoint);
    return locationUpdates
        .take(1)
        .map(CameraUpdate::panAndZoomIn)
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

  public LiveData<Boolean> isLocationUpdatesEnabled() {
    return locationUpdatesEnabled;
  }

  public LiveData<String> getLocationAccuracy() {
    return locationAccuracy;
  }

  public LiveData<Integer> getIconTint() {
    return iconTint;
  }

  private boolean isLocationLockEnabled() {
    return locationLockState.getValue().isTrue();
  }

  public void onCameraMove(CameraPosition newCameraPosition) {
    Timber.d("Setting position to %s", newCameraPosition.toString());
    cameraPosition.setValue(newCameraPosition);
    Loadable.getValue(projectLoadingState)
        .ifPresent(
            project -> projectRepository.setCameraPosition(project.getId(), newCameraPosition));
  }

  public void onMapDrag() {
    if (isLocationLockEnabled()) {
      Timber.d("User dragged map. Disabling location lock");
      locationLockChangeRequests.onNext(false);
    }
  }

  public void onMarkerClick(MapPin pin) {
    panAndZoomCamera(pin.getPosition());
  }

  public void panAndZoomCamera(CameraPosition cameraPosition) {
    cameraUpdateSubject.onNext(CameraUpdate.panAndZoom(cameraPosition));
  }

  public void panAndZoomCamera(Point position) {
    cameraUpdateSubject.onNext(CameraUpdate.panAndZoomIn(position));
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
    mapControlsVisibility.postValue(viewMode == Mode.DEFAULT ? VISIBLE : GONE);
    moveFeaturesVisibility.postValue(viewMode == Mode.REPOSITION ? VISIBLE : GONE);
  }

  public void onMapTypeButtonClicked() {
    selectMapTypeClicks.onNext(Nil.NIL);
  }

  public void onAddFeatureBtnClick() {
    addFeatureButtonClicks.onNext(getCameraPosition().getValue().getTarget());
  }

  public Observable<Nil> getSelectMapTypeClicks() {
    return selectMapTypeClicks;
  }

  public Observable<Point> getAddFeatureButtonClicks() {
    return addFeatureButtonClicks;
  }

  public LiveData<Integer> getMapControlsVisibility() {
    return mapControlsVisibility;
  }

  public LiveData<Integer> getMoveFeatureVisibility() {
    return moveFeaturesVisibility;
  }

  public LiveData<Boolean> isAddPolygonButtonVisible() {
    return addPolygonVisible;
  }

  public LiveData<Boolean> getPolygonDrawingCompletedVisibility() {
    return completeButtonVisible;
  }

  public Optional<Feature> getReposFeature() {
    return reposFeature;
  }

  public void setReposFeature(Optional<Feature> reposFeature) {
    this.reposFeature = reposFeature;
  }

  /** Called when a feature is (de)selected. */
  public void setSelectedFeature(Optional<Feature> selectedFeature) {
    this.selectedFeature.onNext(selectedFeature);
  }

  public void setFeatureButtonBackgroundTint(@ColorRes int colorRes) {
    featureAddButtonBackgroundTint.postValue(colorRes);
  }

  public LiveData<Integer> getFeatureAddButtonBackgroundTint() {
    return featureAddButtonBackgroundTint;
  }

  public LiveData<Boolean> getLocationLockEnabled() {
    return locationLockEnabled;
  }

  public void setLocationLockEnabled(boolean enabled) {
    locationLockEnabled.postValue(enabled);
  }

  public enum Mode {
    DEFAULT,
    REPOSITION
  }

  static class CameraUpdate {

    private final Point center;
    private final Optional<Float> zoomLevel;
    private final boolean allowZoomOut;

    public CameraUpdate(Point center, Optional<Float> zoomLevel, boolean allowZoomOut) {
      this.center = center;
      this.zoomLevel = zoomLevel;
      this.allowZoomOut = allowZoomOut;
    }

    private static CameraUpdate pan(Point center) {
      return new CameraUpdate(center, Optional.empty(), false);
    }

    private static CameraUpdate panAndZoomIn(Point center) {
      return new CameraUpdate(center, Optional.of(DEFAULT_FEATURE_ZOOM_LEVEL), false);
    }

    public static CameraUpdate panAndZoom(CameraPosition cameraPosition) {
      return new CameraUpdate(
          cameraPosition.getTarget(), Optional.of(cameraPosition.getZoomLevel()), true);
    }

    public Point getCenter() {
      return center;
    }

    public Optional<Float> getZoomLevel() {
      return zoomLevel;
    }

    public boolean isAllowZoomOut() {
      return allowZoomOut;
    }

    @NonNull
    @Override
    public String toString() {
      if (zoomLevel.isPresent()) {
        return "Pan + zoom";
      } else {
        return "Pan";
      }
    }
  }
}
