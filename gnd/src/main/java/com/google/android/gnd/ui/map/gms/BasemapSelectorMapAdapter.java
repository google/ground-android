package com.google.android.gnd.ui.map.gms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.ui.map.Extent;
import com.google.android.gnd.ui.map.ExtentSelector;
import com.google.common.collect.ImmutableSet;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import static com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION;
import static java8.util.stream.StreamSupport.stream;

public class BasemapSelectorMapAdapter implements ExtentSelector {

  private static final String TAG = GoogleMapsMapAdapter.class.getSimpleName();
  private static final String GEO_JSON_FILE = "gnd-geojson.geojson";
  private final GoogleMap map;
  private final Context context;

  private final PublishSubject<Point> dragInteractionSubject = PublishSubject.create();
  private final BehaviorSubject<Point> cameraPositionSubject = BehaviorSubject.create();
  private final PublishSubject<Extent> extentsSubject = PublishSubject.create();

  @Nullable private LatLng cameraTargetBeforeDrag;
  private GeoJsonLayer extentSelectionLayer;
  private HashMap<String, Extent> availableExtents;

  public BasemapSelectorMapAdapter(GoogleMap map, Context context) {
    this.map = map;
    this.context = context;
    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    UiSettings uiSettings = map.getUiSettings();
    uiSettings.setRotateGesturesEnabled(false);
    uiSettings.setTiltGesturesEnabled(false);
    uiSettings.setMyLocationButtonEnabled(false);
    uiSettings.setMapToolbarEnabled(false);
    uiSettings.setCompassEnabled(false);
    uiSettings.setIndoorLevelPickerEnabled(false);
    map.setOnCameraIdleListener(this::onCameraIdle);
    map.setOnCameraMoveStartedListener(this::onCameraMoveStarted);
    map.setOnCameraMoveListener(this::onCameraMove);
    onCameraMove();
  }

  @Override
  public void renderExtentSelectionLayer() {
    File file = new File(context.getFilesDir(), GEO_JSON_FILE);

    try {
      InputStream is = new FileInputStream(file);
      BufferedReader buf = new BufferedReader(new InputStreamReader(is));
      String line = buf.readLine();
      StringBuilder sb = new StringBuilder();

      // TODO: Use a higher-level API to read the contents of the JSON file.
      while (line != null) {
        sb.append(line).append("\n");
        line = buf.readLine();
      }

      JSONObject geoJson = new JSONObject(sb.toString());
      this.extentSelectionLayer = new GeoJsonLayer(map, geoJson);
      extentSelectionLayer.addLayerToMap();
      extentSelectionLayer.setOnFeatureClickListener(this::onExtentSelection);

      Iterable<GeoJsonFeature> jsonFeatures = this.extentSelectionLayer.getFeatures();

      for (GeoJsonFeature feature : jsonFeatures) {
        availableExtents.put(
            feature.getId(),
            Extent.newBuilder().setId(feature.getId()).setState(Extent.State.NONE).build());
      }

      Log.d(TAG, "Extent selection layer successfully loaded");

    } catch (IOException | JSONException e) {
      Log.e(TAG, "Unable to load extent selection layer", e);
    }
  }

  @Override
  public void updateExtentSelections(ImmutableSet<Extent> extents) {
    stream(extents.asList())
        .filter(extent -> availableExtents.containsKey(extent))
        .forEach(this::updateExtentSelectionState);
  }

  private void updateExtentSelectionState(Extent extent) {
    availableExtents.put(extent.getId(), extent);
    extentsSubject.onNext(extent);
  }

  private void onExtentSelection(Feature feature) {
    GeoJsonFeature geoJsonFeature = (GeoJsonFeature) feature;
    Extent extent = availableExtents.get(geoJsonFeature.getId());

    // TODO: Refactor repetitive extent building.
    switch (extent.getState()) {
      case DOWNLOADED:
        updateExtentSelectionState(
            extent.toBuilder().setState(Extent.State.PENDING_REMOVAL).build());
        break;
      case PENDING_DOWNLOAD:
        updateExtentSelectionState(extent.toBuilder().setState(Extent.State.NONE).build());
        break;
      case PENDING_REMOVAL:
        updateExtentSelectionState(extent.toBuilder().setState(Extent.State.DOWNLOADED).build());
        break;
      case NONE:
        updateExtentSelectionState(
            extent.toBuilder().setState(Extent.State.PENDING_DOWNLOAD).build());
        break;
    }
  }

  @Override
  public Observable<Extent> getExtentSelections() {
    return Observable.empty();
  }

  @Override
  public Observable<Point> getDragInteractions() {
    return dragInteractionSubject;
  }

  @Override
  public Observable<Point> getCameraPosition() {
    return cameraPositionSubject;
  }

  @Override
  public void enable() {
    map.getUiSettings().setAllGesturesEnabled(true);
  }

  @Override
  public void disable() {
    map.getUiSettings().setAllGesturesEnabled(false);
  }

  @Override
  public void moveCamera(Point point) {
    map.moveCamera(CameraUpdateFactory.newLatLng(point.toLatLng()));
  }

  @Override
  public void moveCamera(Point point, float zoomLevel) {
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(point.toLatLng(), zoomLevel));
  }

  @Override
  public Point getCenter() {
    return Point.fromLatLng(map.getCameraPosition().target);
  }

  @Override
  public float getCurrentZoomLevel() {
    return map.getCameraPosition().zoom;
  }

  @Override
  @SuppressLint("MissingPermission")
  public void enableCurrentLocationIndicator() {
    if (!map.isMyLocationEnabled()) {
      map.setMyLocationEnabled(true);
    }
  }

  private void onCameraIdle() {
    cameraTargetBeforeDrag = null;
  }

  private void onCameraMoveStarted(int reason) {
    if (reason == REASON_DEVELOPER_ANIMATION) {
      // MapAdapter was panned by the app, not the user.
      return;
    }
    cameraTargetBeforeDrag = map.getCameraPosition().target;
  }

  private void onCameraMove() {
    LatLng cameraTarget = map.getCameraPosition().target;
    Point target = Point.fromLatLng(cameraTarget);
    cameraPositionSubject.onNext(target);
    if (cameraTargetBeforeDrag != null && !cameraTarget.equals(cameraTargetBeforeDrag)) {
      dragInteractionSubject.onNext(target);
    }
  }
}
