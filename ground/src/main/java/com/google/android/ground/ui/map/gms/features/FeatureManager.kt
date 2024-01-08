/*
 * Copyright 2024 Google LLC
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

package com.google.android.ground.ui.map.gms.features

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.ground.ui.map.Feature
import com.google.maps.android.clustering.Cluster
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

/** This class is not thread-safe. */
class FeatureManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val pointRenderer: PointRenderer,
  private val polygonRenderer: PolygonRenderer,
  private val lineStringRenderer: LineStringRenderer
) {
  private val features = mutableSetOf<Feature>()
  private val featuresByTag = mutableMapOf<Feature.Tag, Feature>()

  private lateinit var map: GoogleMap
  private lateinit var mapsItemManager: MapsItemManager
  private lateinit var clusterManager: FeatureClusterManager
  private lateinit var clusterRenderer: FeatureClusterRenderer

  // This must be set rather than read from `map.cameraPosition` since that object is only
  // accessible on the UI thread.
  var zoom: Float
    get() = clusterRenderer.zoom
    set(value) {
      clusterRenderer.zoom = value
    }

  fun onMapReady(map: GoogleMap) {
    features.clear()
    featuresByTag.clear()
    mapsItemManager = MapsItemManager(map, pointRenderer, polygonRenderer, lineStringRenderer)
    clusterManager = FeatureClusterManager(context, map)
    clusterRenderer = FeatureClusterRenderer(context, map, clusterManager, map.cameraPosition.zoom)
    // TODO(!!!): Markers are intermittently not rendered after zooming in.
    clusterRenderer.onClusterItemRendered = { mapsItemManager.setVisible(it, true) }
    clusterRenderer.onClusterRendered = { mapsItemManager.setVisible(it, false) }
    clusterManager.renderer = clusterRenderer
    this.map = map
  }

  fun setFeatures(updatedFeatures: Collection<Feature>) {
    // remove stale
    val removedOrChanged = features - updatedFeatures.toSet()
    removedOrChanged.forEach(this::remove)
    // add missing
    val newOrChanged = updatedFeatures - features
    newOrChanged.forEach { add(it) }
    // cluster and update visibility
    clusterManager.cluster()
    Timber.v("${removedOrChanged.size} features removed, ${newOrChanged.size} added")
  }

  fun getIntersectingPolygons(latLng: LatLng): Set<Feature> =
    mapsItemManager.getIntersectingPolygonTags(latLng).mapNotNull { featuresByTag[it] }.toSet()

  private fun add(feature: Feature) =
    with(feature) {
      features.add(this)
      featuresByTag[tag] = this
      if (clusterable) clusterManager.addFeature(this)
      mapsItemManager.add(feature, visible = !clusterable)
    }

  private fun remove(feature: Feature) =
    with(feature) {
      features.remove(this)
      featuresByTag.remove(tag)
      mapsItemManager.remove(tag)
      clusterManager.removeFeature(tag)
    }

  fun onCameraIdle() {
    clusterManager.onCameraIdle()
  }
}
