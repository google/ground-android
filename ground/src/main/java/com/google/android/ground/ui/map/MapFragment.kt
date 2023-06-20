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
package com.google.android.ground.ui.map

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractFragment
import io.reactivex.Flowable
import io.reactivex.Observable
import java8.util.function.Consumer

/** Interface for a Fragment that renders a map view. */
interface MapFragment {
  /** Returns a list of supported basemap types. */
  val availableMapTypes: Array<MapType>

  /** Returns the current map zoom level. */
  val currentZoomLevel: Float

  /** Get or set the current map type. */
  var mapType: Int

  /** Get or set the bounds of the currently visible viewport. */
  var viewport: Bounds

  /** A stream of interaction events on rendered location of interest [Feature]s. */
  val locationOfInterestInteractions: @Hot Observable<List<Feature>>

  /**
   * Returns map drag events. Emits an empty event when the map starts to move by the user.
   * Subscribers that can't keep up receive the latest event ([Flowable.onBackpressureLatest]).
   */
  val startDragEvents: @Hot Flowable<Nil>

  /**
   * Returns camera movement events. Emits the new camera position each time the map stops moving.
   * Subscribers that can't keep up receive the latest event ([Flowable.onBackpressureLatest]).
   */
  val cameraMovedEvents: @Hot Flowable<CameraPosition>

  // TODO(#691): Create interface and impl to encapsulate MapBoxOfflineTileProvider impl.
  /** Returns TileProviders associated with this map adapter. */
  val tileProviders: @Hot Observable<MapBoxOfflineTileProvider>

  /** Adds the [MapFragment] to a fragment. */
  fun attachToFragment(
    containerFragment: AbstractFragment,
    @IdRes containerId: Int,
    mapAdapter: Consumer<MapFragment>
  )

  /** Enables map gestures like pan and zoom. */
  fun enableGestures()

  /** Disables all map gestures like pan and zoom. */
  fun disableGestures()

  /** Centers the map viewport around the specified [Coordinate]. */
  fun moveCamera(coordinate: Coordinate)

  /**
   * Centers the map viewport around the specified [Coordinate] and updates the map's current zoom
   * level.
   */
  fun moveCamera(coordinate: Coordinate, zoomLevel: Float)

  /**
   * Centers the map viewport around the specified [Bounds] are included at the least zoom level.
   */
  fun moveCamera(bounds: Bounds)

  /** Displays user location indicator on the map. */
  @SuppressLint("MissingPermission") fun enableCurrentLocationIndicator()

  /** Update the set of map [Feature]s rendered on the map. */
  fun renderFeatures(features: Set<Feature>)

  fun refresh()

  /** Render locally stored tile overlays on the map. */
  fun addLocalTileOverlays(mbtilesFiles: Set<String>)

  /** Render remote tile overlays on the map. */
  fun addRemoteTileOverlays(urls: List<String>)

  /** Returns the actual distance in pixels between provided [Coordinate]s. */
  fun getDistanceInPixels(coordinate1: Coordinate, coordinate2: Coordinate): Double

  /** Update UI of rendered [LocationOfInterest]. */
  fun setActiveLocationOfInterest(newLoiId: String?)
}
