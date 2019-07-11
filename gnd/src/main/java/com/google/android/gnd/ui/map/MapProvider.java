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
import androidx.fragment.app.Fragment;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Point;
import com.google.common.collect.ImmutableSet;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.Single;

/** Common interface for various map provider libraries. */
public interface MapProvider {
  void restore(Fragment fragment);

  Fragment getFragment();

  Single<MapAdapter> getMapAdapter();

  /**
   * Interface defining map interactions and events. This a separate class from {@link MapProvider}
   * so that it can be returned asynchronously by {@link MapProvider#getMapAdapter()} if necessary.
   */
  interface MapAdapter {

    Observable<MapMarker> getMarkerClicks();

    Observable<Point> getDragInteractions();

    Observable<Point> getCameraPosition();

    void enable();

    void disable();

    void moveCamera(Point point);

    void moveCamera(Point point, float zoomLevel);

    Point getCenter();

    float getCurrentZoomLevel();

    @SuppressLint("MissingPermission")
    void enableCurrentLocationIndicator();

    void updateMarkers(ImmutableSet<Feature> features);

    void renderOfflineTileSet(File file);
  }
}
