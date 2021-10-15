/*
 * Copyright 2021 Google LLC
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

package com.google.android.gnd.ui.map;

import android.annotation.SuppressLint;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import java8.util.function.Consumer;

/** Interface for a Fragment that renders a map view. */
public interface MapFragment {

  /** Returns a list of supported basemap types. */
  ImmutableList<MapType> getAvailableMapTypes();

  /** Adds the {@link MapFragment} to a fragment. */
  void attachToFragment(
      @NonNull AbstractFragment containerFragment,
      @IdRes int containerId,
      @NonNull Consumer<MapFragment> mapAdapter);

  /** Returns marker click events. */
  @Hot
  Observable<MapPin> getMapPinClicks();

  @Hot
  Observable<ImmutableList<MapFeature>> getFeatureClicks();

  /**
   * Returns map drag events. Emits an empty event when the map starts to move by the user.
   * Subscribers that can't keep up receive the latest event ({@link
   * Flowable#onBackpressureLatest()}).
   */
  @Hot
  Flowable<Nil> getStartDragEvents();

  /**
   * Returns camera move events. Emits the new camera position each time the map stops moving.
   * Subscribers that can't keep up receive the latest event ({@link
   * Flowable#onBackpressureLatest()}).
   */
  @Hot
  Flowable<CameraPosition> getCameraMovedEvents();

  /** Enables map gestures like pan and zoom. */
  void enableGestures();

  /** Disables all map gestures like pan and zoom. */
  void disableGestures();

  /**
   * Repositions the viewport centered around the specified point without changing the current zoom
   * level.
   */
  void moveCamera(Point point);

  /**
   * Repositions the viewport centered around the specified point while also updating the current
   * zoom level.
   */
  void moveCamera(Point point, float zoomLevel);

  /** Returns the current map zoom level. */
  float getCurrentZoomLevel();

  /** Displays user location indicator on the map. */
  @SuppressLint("MissingPermission")
  void enableCurrentLocationIndicator();

  /** Update map pins/polygons shown on map. */
  void setMapFeatures(ImmutableSet<MapFeature> mapFeatures);

  /** Get current map type. */
  int getMapType();

  /** Update map type. */
  void setMapType(int mapType);

  /** Returns the bounds of the currently visible viewport. */
  LatLngBounds getViewport();

  /** Set the map viewport to the given bounds. */
  void setViewport(LatLngBounds bounds);

  /** Renders a tile overlay on the map. */
  void addLocalTileOverlays(ImmutableSet<String> mbtilesFiles);

  /** Renders a remote tile overlays on the map. */
  void addRemoteTileOverlays(ImmutableList<String> urls);

  // TODO(#691): Create interface and impl to encapsulate MapBoxOfflineTileProvider impl.
  /** Returns TileProviders associated with this map adapter. */
  @Hot
  Observable<MapBoxOfflineTileProvider> getTileProviders();
}
