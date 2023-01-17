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
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractFragment
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.Flowable
import io.reactivex.Observable
import java8.util.function.Consumer

/** Interface for a Fragment that renders a map view. */
interface MapFragment {
  /** Returns a list of supported basemap types. */
  val availableMapTypes: ImmutableList<MapType>

  /** Returns the current map zoom level. */
  val currentZoomLevel: Float

  /** Get or set the current map type. */
  var mapType: Int

  /** Get or set the bounds of the currently visible viewport. */
  var viewport: Bounds

  /** Adds the [MapFragment] to a fragment. */
  fun attachToFragment(
    containerFragment: AbstractFragment,
    @IdRes containerId: Int,
    mapAdapter: Consumer<MapFragment>
  )

  /** A stream of interaction events on rendered location of interest [Feature]s. */
  val locationOfInterestInteractions: @Hot Observable<ImmutableList<Feature>>

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

  /** Enables map gestures like pan and zoom. */
  fun enableGestures()

  /** Disables all map gestures like pan and zoom. */
  fun disableGestures()

  /** Centers the map viewport around the specified [Point]. */
  fun moveCamera(point: Point)

  /**
   * Centers the map viewport around the specified [Point] and updates the map's current zoom level.
   */
  fun moveCamera(point: Point, zoomLevel: Float)

  /** Displays user location indicator on the map. */
  @SuppressLint("MissingPermission") fun enableCurrentLocationIndicator()

  /** Update the set of map [Feature]s rendered on the map. */
  fun renderFeatures(features: ImmutableSet<Feature>)

  fun refresh()

  // TODO(#691): Create interface and impl to encapsulate MapBoxOfflineTileProvider impl.
  /** Returns TileProviders associated with this map adapter. */
  val tileProviders: @Hot Observable<MapBoxOfflineTileProvider>

  /** Render locally stored tile overlays on the map. */
  fun addLocalTileOverlays(mbtilesFiles: ImmutableSet<String>)

  /** Render remote tile overlays on the map. */
  fun addRemoteTileOverlays(urls: ImmutableList<String>)

  /** Returns the actual distance in pixels between provided points. */
  fun getDistanceInPixels(point1: Point, point2: Point): Double

  /** Update UI of rendered [LocationOfInterest]. */
  fun setActiveLocationOfInterest(locationOfInterest: LocationOfInterest?)
}
