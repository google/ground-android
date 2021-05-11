/*
 * Copyright 2020 Google LLC
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
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.map.tileprovider.CloseableTileProvider;
import com.google.android.gnd.ui.map.tileprovider.LocalTileProvider;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * Interface defining map interactions and events. This a separate class from {@link MapProvider} so
 * that it can be returned asynchronously by {@link MapProvider#getMapAdapter()} if necessary.
 */
public interface MapAdapter {

  /** Returns marker click events. */
  @Hot
  Observable<MapPin> getMapPinClicks();

  /**
   * Returns map drag events. Emits the new viewport center each time the map is dragged by the
   * user. Subscribers that can't keep up receive the latest event ({@link
   * Flowable#onBackpressureLatest()}).
   */
  @Hot
  Flowable<Point> getDragInteractions();

  /**
   * Returns camera move events. Emits the new camera position each time the map pans or zooms.
   * Subscribers that can't keep up receive the latest event ({@link
   * Flowable#onBackpressureLatest()}).
   */
  @Hot
  Flowable<CameraPosition> getCameraMoves();

  /** Enables map gestures like pan and zoom. */
  void enable();

  /** Disables all map gestures like pan and zoom. */
  void disable();

  /**
   * Repositions the camera.
   *
   * @param position the new position
   */
  void moveCamera(CameraPosition position);

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

  /** Returns the current center of the viewport. */
  Point getCameraTarget();

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

  /** Returns the bounds of the currently visibly viewport. */
  LatLngBounds getViewport();

  /** Set the map viewport to the given bounds. */
  void setBounds(LatLngBounds bounds);

  // TODO(#691): Create interface and impl to encapsulate MapBoxOfflineTileProvider impl.
  /** Returns TileProviders associated with this map adapter. */
  @Hot
  Observable<LocalTileProvider> getTileProviders();

  void addTileOverlay(LocalTileProvider tileProvider);
}
