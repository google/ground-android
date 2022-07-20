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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.android.ground.util.ImmutableSetCollector.toImmutableSet;
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
import com.google.android.ground.R;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.basemap.tile.TileSet;
import com.google.android.ground.model.job.Style;
import com.google.android.ground.model.locationofinterest.AreaOfInterest;
import com.google.android.ground.model.locationofinterest.GeoJsonLocationOfInterest;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.locationofinterest.Point;
import com.google.android.ground.model.locationofinterest.PointOfInterest;
import com.google.android.ground.repository.LocationOfInterestRepository;
import com.google.android.ground.repository.OfflineAreaRepository;
import com.google.android.ground.repository.SurveyRepository;
import com.google.android.ground.rx.BooleanOrError;
import com.google.android.ground.rx.Event;
import com.google.android.ground.rx.Loadable;
import com.google.android.ground.rx.Nil;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.system.LocationManager;
import com.google.android.ground.ui.common.AbstractViewModel;
import com.google.android.ground.ui.common.SharedViewModel;
import com.google.android.ground.ui.map.CameraPosition;
import com.google.android.ground.ui.map.MapGeoJson;
import com.google.android.ground.ui.map.MapLocationOfInterest;
import com.google.android.ground.ui.map.MapPin;
import com.google.android.ground.ui.map.MapPolygon;
import com.google.common.collect.ImmutableSet;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.processors.BehaviorProcessor;
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

  // Higher zoom levels means the map is more zoomed in. 0.0f is fully zoomed out.
  public static final float ZOOM_LEVEL_THRESHOLD = 16f;
  public static final float DEFAULT_LOI_ZOOM_LEVEL = 18.0f;
  private static final float DEFAULT_MAP_ZOOM_LEVEL = 0.0f;
  private static final Point DEFAULT_MAP_POINT =
      Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build();
  private final LiveData<Loadable<Survey>> surveyLoadingState;
  private final LiveData<ImmutableSet<MapLocationOfInterest>> mapLocationsOfInterest;
  private final LiveData<BooleanOrError> locationLockState;
  private final LiveData<Event<CameraUpdate>> cameraUpdateRequests;

  @Hot(replays = true)
  private final MutableLiveData<CameraPosition> cameraPosition =
      new MutableLiveData<>(new CameraPosition(DEFAULT_MAP_POINT, DEFAULT_MAP_ZOOM_LEVEL));

  private final Resources resources;
  private final SurveyRepository surveyRepository;
  private final LocationManager locationManager;
  private final LocationOfInterestRepository locationOfInterestRepository;

  @Hot private final Subject<Boolean> locationLockChangeRequests = PublishSubject.create();
  @Hot private final Subject<CameraUpdate> cameraUpdateSubject = PublishSubject.create();

  /**
   * Temporary set of {@link MapLocationOfInterest} used for displaying on map during add/edit
   * flows.
   */
  @Hot
  private final PublishProcessor<ImmutableSet<MapLocationOfInterest>>
      unsavedMapLocationsOfInterest = PublishProcessor.create();

  @Hot(replays = true)
  private final MutableLiveData<Integer> mapControlsVisibility = new MutableLiveData<>(VISIBLE);

  private final MutableLiveData<Integer> addPolygonVisibility = new MutableLiveData<>(GONE);

  @Hot(replays = true)
  private final MutableLiveData<Integer> moveLocationsOfInterestVisibility =
      new MutableLiveData<>(GONE);

  @Hot(replays = true)
  private final MutableLiveData<Boolean> locationLockEnabled = new MutableLiveData<>();

  @Hot(replays = true)
  private final MutableLiveData<Integer> locationOfInterestAddButtonBackgroundTint =
      new MutableLiveData<>(R.color.colorGrey500);

  private final LiveData<ImmutableSet<String>> mbtilesFilePaths;
  private final LiveData<Integer> iconTint;
  private final LiveData<Boolean> locationUpdatesEnabled;
  private final LiveData<String> locationAccuracy;
  private final List<MapBoxOfflineTileProvider> tileProviders = new ArrayList<>();
  private final @Dimension int defaultPolygonStrokeWidth;
  private final @Dimension int selectedPolygonStrokeWidth;
  /** The currently selected LOI on the map. */
  private final BehaviorProcessor<Optional<LocationOfInterest>> selectedLocationOfInterest =
      BehaviorProcessor.createDefault(Optional.empty());

  /* UI Clicks */
  @Hot private final Subject<Nil> selectMapTypeClicks = PublishSubject.create();

  @Hot private final Subject<Nil> zoomThresholdCrossed = PublishSubject.create();
  // TODO: Move this in LocationOfInterestRepositionView and return the final updated LOI as the
  // result.
  /** LocationOfInterest selected for repositioning. */
  private Optional<LocationOfInterest> reposLocationOfInterest = Optional.empty();

  @Inject
  MapContainerViewModel(
      Resources resources,
      SurveyRepository surveyRepository,
      LocationOfInterestRepository locationOfInterestRepository,
      LocationManager locationManager,
      OfflineAreaRepository offlineAreaRepository) {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    this.resources = resources;
    this.surveyRepository = surveyRepository;
    this.locationOfInterestRepository = locationOfInterestRepository;
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
    this.surveyLoadingState =
        LiveDataReactiveStreams.fromPublisher(surveyRepository.getSurveyLoadingState());
    // TODO: Clear location of interest markers when survey is deactivated.
    // TODO: Since we depend on survey stream from repo anyway, this transformation can be moved
    // into the repo?
    // LOIs that are persisted to the local and remote dbs.
    Flowable<ImmutableSet<MapLocationOfInterest>> savedMapLocationsOfInterest =
        Flowable.combineLatest(
            surveyRepository
                .getActiveSurvey()
                .switchMap(this::getLocationsOfInterestStream)
                .map(this::toMapLocationsOfInterest),
            selectedLocationOfInterest,
            this::updateSelectedLocationOfInterest);

    this.mapLocationsOfInterest =
        LiveDataReactiveStreams.fromPublisher(
            Flowable.combineLatest(
                    Arrays.asList(
                        savedMapLocationsOfInterest.startWith(
                            ImmutableSet.<MapLocationOfInterest>of()),
                        unsavedMapLocationsOfInterest.startWith(
                            ImmutableSet.<MapLocationOfInterest>of())),
                    MapContainerViewModel::concatLocationsOfInterestSets)
                .distinctUntilChanged());

    this.mbtilesFilePaths =
        LiveDataReactiveStreams.fromPublisher(
            offlineAreaRepository
                .getDownloadedTileSetsOnceAndStream()
                .map(set -> stream(set).map(TileSet::getPath).collect(toImmutableSet())));
    disposeOnClear(surveyRepository.getActiveSurvey().subscribe(this::onSurveyChange));
  }

  private static ImmutableSet<MapLocationOfInterest> concatLocationsOfInterestSets(
      Object[] objects) {
    return stream(Arrays.asList(objects))
        .flatMap(set -> stream((ImmutableSet<MapLocationOfInterest>) set))
        .collect(toImmutableSet());
  }

  private static MapLocationOfInterest toMapPin(PointOfInterest pointOfInterest) {
    return MapPin.newBuilder()
        .setId(pointOfInterest.getId())
        .setPosition(pointOfInterest.getPoint())
        .setStyle(Style.DEFAULT_MAP_STYLE)
        .setLocationOfInterest(pointOfInterest)
        .build();
  }

  private static MapLocationOfInterest toMapPolygon(AreaOfInterest areaOfInterest) {
    return MapPolygon.newBuilder()
        .setId(areaOfInterest.getId())
        .setVertices(areaOfInterest.getVertices())
        .setStyle(Style.DEFAULT_MAP_STYLE)
        .setLocationOfInterest(areaOfInterest)
        .build();
  }

  private void onSurveyChange(Optional<Survey> project) {
    project
        .map(Survey::getId)
        .flatMap(surveyRepository::getLastCameraPosition)
        .ifPresent(this::panAndZoomCamera);
  }

  public void setUnsavedMapLocationsOfInterest(
      ImmutableSet<MapLocationOfInterest> locationsOfInterest) {
    unsavedMapLocationsOfInterest.onNext(locationsOfInterest);
  }

  private ImmutableSet<MapLocationOfInterest> updateSelectedLocationOfInterest(
      ImmutableSet<MapLocationOfInterest> locationsOfInterest,
      Optional<LocationOfInterest> selectedLocationOfInterest) {
    Timber.v("Updating selected LOI style");
    if (selectedLocationOfInterest.isEmpty()) {
      return locationsOfInterest;
    }
    ImmutableSet.Builder updatedLocationsOfInterest = ImmutableSet.builder();
    String selectedLocationOfInterestId = selectedLocationOfInterest.get().getId();
    for (MapLocationOfInterest locationOfInterest : locationsOfInterest) {
      if (locationOfInterest instanceof MapGeoJson) {
        MapGeoJson geoJsonLocationOfInterest = (MapGeoJson) locationOfInterest;
        String geoJsonLocationOfInterestId =
            geoJsonLocationOfInterest.getLocationOfInterest().getId();
        if (geoJsonLocationOfInterestId.equals(selectedLocationOfInterestId)) {
          Timber.v(
              "Restyling selected GeoJSON location of interest " + selectedLocationOfInterestId);
          updatedLocationsOfInterest.add(
              geoJsonLocationOfInterest.toBuilder()
                  .setStrokeWidth(selectedPolygonStrokeWidth)
                  .build());
          continue;
        }
      }
      updatedLocationsOfInterest.add(locationOfInterest);
    }
    return updatedLocationsOfInterest.build();
  }

  private ImmutableSet<MapLocationOfInterest> toMapLocationsOfInterest(
      ImmutableSet<LocationOfInterest> locationsOfInterest) {
    ImmutableSet<MapLocationOfInterest> mapPins =
        stream(locationsOfInterest)
            .filter(LocationOfInterest::isPoint)
            .map(PointOfInterest.class::cast)
            .map(MapContainerViewModel::toMapPin)
            .collect(toImmutableSet());

    // TODO: Add support for polylines similar to mapPins.

    ImmutableSet<MapLocationOfInterest> mapGeoJson =
        stream(locationsOfInterest)
            .filter(LocationOfInterest::isGeoJson)
            .map(GeoJsonLocationOfInterest.class::cast)
            .map(this::toMapGeoJson)
            .collect(toImmutableSet());

    ImmutableSet<MapLocationOfInterest> mapPolygons =
        stream(locationsOfInterest)
            .filter(LocationOfInterest::isPolygon)
            .map(AreaOfInterest.class::cast)
            .map(MapContainerViewModel::toMapPolygon)
            .collect(toImmutableSet());

    return ImmutableSet.<MapLocationOfInterest>builder()
        .addAll(mapPins)
        .addAll(mapGeoJson)
        .addAll(mapPolygons)
        .build();
  }

  private MapGeoJson toMapGeoJson(GeoJsonLocationOfInterest geoJsonLocationOfInterest) {
    JSONObject jsonObject;
    try {
      jsonObject = new JSONObject(geoJsonLocationOfInterest.getGeoJsonString());
    } catch (JSONException e) {
      Timber.e(e);
      jsonObject = new JSONObject();
    }

    return MapGeoJson.newBuilder()
        .setId(geoJsonLocationOfInterest.getId())
        .setGeoJson(jsonObject)
        .setStyle(Style.DEFAULT_MAP_STYLE)
        .setStrokeWidth(defaultPolygonStrokeWidth)
        .setLocationOfInterest(geoJsonLocationOfInterest)
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
        locationManager.getLocationUpdates().map(MapContainerViewModel::toPoint);
    return locationUpdates
        .take(1)
        .map(CameraUpdate::panAndZoomIn)
        .concatWith(locationUpdates.map(CameraUpdate::pan).skip(1));
  }

  private static Point toPoint(Location location) {
    return Point.newBuilder()
        .setLatitude(location.getLatitude())
        .setLongitude(location.getLongitude())
        .build();
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

  private Flowable<ImmutableSet<LocationOfInterest>> getLocationsOfInterestStream(
      Optional<Survey> activeProject) {
    // Emit empty set in separate stream to force unsubscribe from LocationOfInterest updates and
    // update
    // subscribers.
    return activeProject
        .map(locationOfInterestRepository::getLocationsOfInterestOnceAndStream)
        .orElse(Flowable.just(ImmutableSet.of()));
  }

  public LiveData<Loadable<Survey>> getSurveyLoadingState() {
    return surveyLoadingState;
  }

  public LiveData<ImmutableSet<MapLocationOfInterest>> getMapLocationsOfInterest() {
    return mapLocationsOfInterest;
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
    onZoomChange(cameraPosition.getValue().getZoomLevel(), newCameraPosition.getZoomLevel());
    cameraPosition.setValue(newCameraPosition);
    Loadable.getValue(surveyLoadingState)
        .ifPresent(
            project -> surveyRepository.setCameraPosition(project.getId(), newCameraPosition));
  }

  private void onZoomChange(float oldZoomLevel, float newZoomLevel) {
    boolean zoomThresholdCrossed =
        oldZoomLevel < ZOOM_LEVEL_THRESHOLD && newZoomLevel >= ZOOM_LEVEL_THRESHOLD
            || oldZoomLevel >= ZOOM_LEVEL_THRESHOLD && newZoomLevel < ZOOM_LEVEL_THRESHOLD;
    if (zoomThresholdCrossed) {
      this.zoomThresholdCrossed.onNext(Nil.NIL);
    }
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

  public void setMode(Mode viewMode) {
    mapControlsVisibility.postValue(viewMode == Mode.DEFAULT ? VISIBLE : GONE);
    moveLocationsOfInterestVisibility.postValue(viewMode == Mode.MOVE_POINT ? VISIBLE : GONE);
    addPolygonVisibility.postValue(viewMode == Mode.DRAW_POLYGON ? VISIBLE : GONE);

    if (viewMode == Mode.DEFAULT) {
      setReposLocationOfInterest(Optional.empty());
    }
  }

  public void onMapTypeButtonClicked() {
    selectMapTypeClicks.onNext(Nil.NIL);
  }

  public Observable<Nil> getSelectMapTypeClicks() {
    return selectMapTypeClicks;
  }

  public Observable<Nil> getZoomThresholdCrossed() {
    return zoomThresholdCrossed;
  }

  public LiveData<Integer> getMapControlsVisibility() {
    return mapControlsVisibility;
  }

  public LiveData<Integer> getMoveLocationOfInterestVisibility() {
    return moveLocationsOfInterestVisibility;
  }

  public LiveData<Integer> getAddPolygonVisibility() {
    return addPolygonVisibility;
  }

  public Optional<LocationOfInterest> getReposLocationOfInterest() {
    return reposLocationOfInterest;
  }

  public void setReposLocationOfInterest(Optional<LocationOfInterest> reposLocationOfInterest) {
    this.reposLocationOfInterest = reposLocationOfInterest;
  }

  /** Called when a LOI is (de)selected. */
  public void setSelectedLocationOfInterest(
      Optional<LocationOfInterest> selectedLocationOfInterest) {
    this.selectedLocationOfInterest.onNext(selectedLocationOfInterest);
  }

  public void setLocationOfInterestButtonBackgroundTint(@ColorRes int colorRes) {
    locationOfInterestAddButtonBackgroundTint.postValue(colorRes);
  }

  public LiveData<Integer> getLocationOfInterestAddButtonBackgroundTint() {
    return locationOfInterestAddButtonBackgroundTint;
  }

  public LiveData<Boolean> getLocationLockEnabled() {
    return locationLockEnabled;
  }

  public void setLocationLockEnabled(boolean enabled) {
    locationLockEnabled.postValue(enabled);
  }

  public enum Mode {
    DEFAULT,
    DRAW_POLYGON,
    MOVE_POINT,
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
      return new CameraUpdate(center, Optional.of(DEFAULT_LOI_ZOOM_LEVEL), false);
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
