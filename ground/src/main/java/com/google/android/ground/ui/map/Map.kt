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
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractFragment
import io.reactivex.Flowable
import io.reactivex.Observable
import java8.util.function.Consumer

/** Interface for a Fragment that renders a map view. */
interface Map {
  /** A list of map types supported by the map implementation. */
  val supportedMapTypes: List<MapType>

  /** The current map type. */
  var mapType: MapType

  /** The current map zoom level. */
  val currentZoomLevel: Float

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

  /** Adds the [Map] to a fragment. */
  fun attachToFragment(
    containerFragment: AbstractFragment,
    @IdRes containerId: Int,
    onMapReadyCallback: Consumer<Map>
  )

  /** Enables map gestures like pan and zoom. */
  fun enableGestures()

  /** Disables all map gestures like pan and zoom. */
  fun disableGestures()

  /** Centers the map viewport around the specified [Coordinates]. */
  fun moveCamera(coordinates: Coordinates)

  /**
   * Centers the map viewport around the specified [Coordinates] and updates the map's current zoom
   * level.
   */
  fun moveCamera(coordinates: Coordinates, zoomLevel: Float)

  /**
   * Centers the map viewport around the specified [Bounds] are included at the least zoom level.
   */
  fun moveCamera(bounds: Bounds)

  /** Displays user location indicator on the map. */
  @SuppressLint("MissingPermission") fun enableCurrentLocationIndicator()

  /** Update the set of map [Feature]s rendered on the map. */
  fun renderFeatures(features: Set<Feature>)

  fun refresh()

  /** Returns the actual distance in pixels between provided [Coordinates]s. */
  fun getDistanceInPixels(coordinates1: Coordinates, coordinates2: Coordinates): Double

  /** Update UI of rendered [LocationOfInterest]. */
  fun setActiveLocationOfInterest(newLoiId: String?)

  fun addTileOverlay(tileSource: TileSource)

  /** Remove all tile overlays from the map. */
  fun clearTileOverlays()

  /** Removes all markers, overlays, polylines and polygons from the map. */
  fun clear()
}
