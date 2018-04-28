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

package com.google.android.gnd.ui.map;

import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import com.google.android.gnd.model.Point;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Common interface for various map provider libraries.
 */
public interface MapAdapter {
  Fragment getMapFragment();

  Single<MapAdapter.Map> map();

  /**
   * Interface defining map interactions and events. This a separate class from {@link MapAdapter}
   * so that it can be returned asynchronously by {@link MapAdapter#map()} if necessary.
   */
  interface Map {

    Observable<MapMarker> markerClicks();

    Observable<Point> userPans();

    void enable();

    void disable();

    void moveCamera(Point point);

    void moveCamera(Point point, float zoomLevel);

    void addOrUpdateMarker(
        MapMarker mapMarker, boolean hasPendingWrites, boolean isHighlighted);

    void removeMarker(String id);

    void removeAllMarkers();

    Point getCenter();

    float getCurrentZoomLevel();

    @SuppressLint("MissingPermission")
    void enableCurrentLocationIndicator();
  }
}
