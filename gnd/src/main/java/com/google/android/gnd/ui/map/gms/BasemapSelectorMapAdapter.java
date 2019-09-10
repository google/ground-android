package com.google.android.gnd.ui.map.gms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gnd.R;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.ui.map.Extent;
import com.google.android.gnd.ui.map.ExtentSelector;
import com.google.common.collect.ImmutableSet;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java8.util.Optional;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import static com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION;
import static java8.util.stream.StreamSupport.stream;

public class BasemapSelectorMapAdapter implements ExtentSelector {

  private static final String TAG = GoogleMapsMapAdapter.class.getSimpleName();
  private final GoogleMap map;
  private final Context context;

  private final PublishSubject<Point> dragInteractionSubject = PublishSubject.create();
  private final BehaviorSubject<Point> cameraPositionSubject = BehaviorSubject.create();
  private final PublishSubject<Extent> extentsSubject = PublishSubject.create();

  @Nullable private LatLng cameraTargetBeforeDrag;
  private GeoJsonLayer extentSelectionLayer;
  private final HashMap<String, Extent> availableExtents = new HashMap<>();
  private Iterable<GeoJsonFeature> geoJsonFeatures;

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
    int geoJsonResourceId = context.getResources().getIdentifier("gnd_geojson", "raw", context.getPackageName());

    try {
      InputStream is = context.getResources().openRawResource(geoJsonResourceId);
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

      this.geoJsonFeatures = this.extentSelectionLayer.getFeatures();

      for (GeoJsonFeature feature : this.geoJsonFeatures) {
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
        .filter(extent -> availableExtents.containsKey(extent.getId()))
        .forEach(
            extent -> {
              Log.d(TAG, "Updating extent: " + extent);
              this.updateExtentSelectionState(extent);
              this.updateExtentStyle(extent);
            });
  }

  private Optional<GeoJsonFeature> getJsonFromExtent(Extent extent) {
    for (GeoJsonFeature feature : this.geoJsonFeatures) {
      if (feature.getId().equals(extent.getId())) {
        return Optional.of(feature);
      }
    }
    return Optional.empty();
  }

  private void updateExtentStyle(Extent extent) {
    Optional<GeoJsonFeature> maybeFeature = getJsonFromExtent(extent);
    GeoJsonPolygonStyle style = new GeoJsonPolygonStyle();

    if (!maybeFeature.isPresent()) {
      return;
    }

    GeoJsonFeature geoJsonFeature = maybeFeature.get();

    switch (extent.getState()) {
      case DOWNLOADED:
        style.setStrokeWidth(10);
        style.setStrokeColor(ContextCompat.getColor(context, R.color.colorExtentDownloadedStroke));
        style.setFillColor(ContextCompat.getColor(context, R.color.colorExtentDownloadedFill));
        geoJsonFeature.setPolygonStyle(style);
        break;
      case PENDING_DOWNLOAD:
        style.setStrokeWidth(10);
        style.setStrokeColor(
            ContextCompat.getColor(context, R.color.colorExtentPendingDownloadStroke));
        style.setFillColor(ContextCompat.getColor(context, R.color.colorExtentPendingDownloadFill));
        geoJsonFeature.setPolygonStyle(style);
        break;
      case PENDING_REMOVAL:
        style.setStrokeWidth(10);
        style.setStrokeColor(
            ContextCompat.getColor(context, R.color.colorExtentPendingRemovalStroke));
        style.setFillColor(ContextCompat.getColor(context, R.color.colorExtentPendingRemovalFill));
        geoJsonFeature.setPolygonStyle(style);
        break;
      case NONE:
        style.setStrokeWidth(10);
        style.setStrokeColor(ContextCompat.getColor(context, R.color.colorExtentNoneStroke));
        style.setFillColor(ContextCompat.getColor(context, R.color.colorExtentNoneFill));
        geoJsonFeature.setPolygonStyle(style);
        break;
    }
  }

  private void updateExtentSelectionState(Extent extent) {
    availableExtents.put(extent.getId(), extent);
    extentsSubject.onNext(extent);
  }

  private void onExtentSelection(Feature feature) {
    GeoJsonFeature geoJsonFeature = (GeoJsonFeature) feature;
    Extent extent = availableExtents.get(geoJsonFeature.getId());

    Log.d(TAG, "Clicked extent state: " + extent.getState());

    // TODO: Replace all style updates with extent style objects.
    // TODO: Refactor repetitive extent building.
    switch (extent.getState()) {
      case DOWNLOADED:
        updateExtentSelectionState(extent.toBuilder().setState(Extent.State.PENDING_REMOVAL).build());
        updateExtentStyle(extent.toBuilder().setState(Extent.State.PENDING_REMOVAL).build());
        break;
      case PENDING_DOWNLOAD:
        updateExtentSelectionState(extent.toBuilder().setState(Extent.State.NONE).build());
        updateExtentStyle(extent.toBuilder().setState(Extent.State.NONE).build());
        break;
      case PENDING_REMOVAL:
        updateExtentSelectionState(extent.toBuilder().setState(Extent.State.DOWNLOADED).build());
        updateExtentStyle(extent.toBuilder().setState(Extent.State.DOWNLOADED).build());
        break;
      case NONE:
        updateExtentSelectionState(
            extent.toBuilder().setState(Extent.State.PENDING_DOWNLOAD).build());
        updateExtentStyle(extent.toBuilder().setState(Extent.State.PENDING_DOWNLOAD).build());
        break;
    }
  }

  @Override
  public Observable<Extent> getExtentSelections() {
    return extentsSubject;
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
