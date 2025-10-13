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
package org.groundplatform.android.ui.map

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import kotlinx.coroutines.flow.SharedFlow
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.imagery.TileSource
import org.groundplatform.android.model.map.Bounds
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.model.map.MapType
import org.groundplatform.android.ui.common.AbstractFragment

/** Implementation of Fragment which supports displaying a map. */
interface MapFragment {
  /** A list of map types supported by the map implementation. */
  val supportedMapTypes: List<MapType>

  /** The current map type. */
  var mapType: MapType

  /** The current map zoom level. */
  val currentZoomLevel: Float

  /** Get or set the bounds of the currently visible viewport. */
  var viewport: Bounds

  /**
   * Clicks on [Feature] on the maps. A set with multiple items is emitted when multiple overlapping
   * geometries overlap the click location.
   */
  val featureClicks: SharedFlow<Set<Feature>>

  /** Emits as the user begins dragging the map. */
  val startDragEvents: SharedFlow<Unit>

  /**
   * Returns camera movement events. Emits the new camera position each time the map stops moving.
   */
  val cameraMovedEvents: SharedFlow<CameraPosition>

  /** Emits camera target coordinates in real time while the user drags the map. */
  val cameraDragEvents: SharedFlow<Coordinates>

  /** Attaches this [MapFragment] to its parent [Fragment]. */
  fun attachToParent(
    containerFragment: AbstractFragment,
    @IdRes containerId: Int,
    onMapReadyCallback: (MapFragment) -> Unit,
  )

  /** Enables map gestures like pan and zoom. */
  fun enableGestures()

  /** Disables all map gestures like pan and zoom. */
  fun disableGestures()

  /** Enables map gestures for rotation. */
  fun enableRotation()

  /** Disables map gestures for rotation. */
  fun disableRotation()

  /** Centers the map viewport around the specified [Coordinates]. */
  fun moveCamera(coordinates: Coordinates, shouldAnimate: Boolean)

  /**
   * Centers the map viewport around the specified [Coordinates] and updates the map's current zoom
   * level.
   */
  fun moveCamera(coordinates: Coordinates, zoomLevel: Float, shouldAnimate: Boolean)

  /**
   * Centers the map viewport around the specified [Bounds] are included at the least zoom level.
   */
  fun moveCamera(bounds: Bounds, padding: Int, shouldAnimate: Boolean)

  /** Displays user location indicator on the map. */
  @SuppressLint("MissingPermission") fun enableCurrentLocationIndicator()

  /** Update the set of map [Feature]s present on the map. */
  fun setFeatures(newFeatures: Set<Feature>)

  /** Updates an existing [Feature] present on the map. */
  fun updateFeature(feature: Feature)

  /** Returns the actual distance in pixels between provided [Coordinates]s. */
  fun getDistanceInPixels(coordinates1: Coordinates, coordinates2: Coordinates): Double

  fun addTileOverlay(source: TileSource)

  /** Remove all tile overlays from the map. */
  fun clearTileOverlays()

  /** Removes all markers, overlays, polylines and polygons from the map. */
  fun clear()
}
