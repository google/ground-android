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
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.FeatureClusterManager
import com.google.android.ground.ui.map.gms.FeatureClusterRenderer
import com.google.maps.android.PolyUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FeatureManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  pointRenderer: PointRenderer,
  polygonRenderer: PolygonRenderer,
  multiPolygonRenderer: MultiPolygonRenderer,
  lineStringRenderer: LineStringRenderer
) {
  private val features = mutableSetOf<Feature>()
  private val featuresByTag = mutableMapOf<Feature.Tag, Feature>()
  private val pointManager = MapItemManager(pointRenderer)
  private val polygonManager = MapItemManager(polygonRenderer)
  private val multiPolygonManager = MapItemManager(multiPolygonRenderer)
  private val lineStringManager = MapItemManager(lineStringRenderer)
  private val mapItemManagers =
    listOf(pointManager, polygonManager, multiPolygonManager, lineStringManager)

  private lateinit var map: GoogleMap
  private lateinit var clusterManager: FeatureClusterManager
  private lateinit var clusterRenderer: FeatureClusterRenderer
  var zoom: Float
    get() = clusterRenderer.zoom
    set(value) {
      clusterRenderer.zoom = value
    }

  fun onMapReady(map: GoogleMap) {
    this.map = map
    clusterManager = FeatureClusterManager(context, map)
    clusterRenderer =
      FeatureClusterRenderer(
        context,
        map,
        clusterManager,
        this::showItem,
        this::hideItem,
        map.cameraPosition.zoom
      )
    //    clusterManager.setOnClusterClickListener(this::onClusterItemClick) // TODO(!!!): Add
    // callback
    clusterManager.renderer = clusterRenderer
  }

  fun setFeatures(updatedFeatures: Collection<Feature>) {
    // remove stale
    val removedOrChanged = features - updatedFeatures.toSet()
    removedOrChanged.forEach(this::removeFeature)
    // add missing
    val newOrChanged = updatedFeatures - features
    newOrChanged.forEach { addFeature(it) }
  }

  fun getIntersectingPolygons(latLng: LatLng): Set<Feature> {
    val polygons = polygonManager.items + multiPolygonManager.items.flatten()
    return polygons
      .filter { PolyUtil.containsLocation(latLng, it.points, false) }
      .mapNotNull { featuresByTag[it.tag as Feature.Tag] }
      .toSet()
  }

  private fun addFeature(feature: Feature) {
    with(feature) {
      features.add(this)
      featuresByTag[tag] = this
      if (clusterable) clusterManager.addFeature(feature) // TODO(!!!): Encapsulate addItem()
      setMapItem(this, visible = clusterable)
    }
  }

  private fun removeFeature(feature: Feature) {
    with(feature) {
      // Remove from all managers in case geometry type changed.
      mapItemManagers.forEach { it.remove(tag) }
      if (clusterable) clusterManager.removeFeature(tag)
      features.remove(this)
      featuresByTag.remove(tag)
    }
  }

  private fun showItem(tag: Feature.Tag) {
    featuresByTag[tag]?.let { setMapItem(it, true) }
  }

  private fun hideItem(tag: Feature.Tag) {
    featuresByTag[tag]?.let { setMapItem(it, false) }
  }

  private fun setMapItem(feature: Feature, visible: Boolean) =
    with(feature) {
      when (geometry) {
        is Point -> pointManager.set(map, tag, geometry, style, visible)
        is LineString -> lineStringManager.set(map, tag, geometry, style, visible)
        is LinearRing -> error("LinearRing rendering not supported")
        is MultiPolygon -> multiPolygonManager.set(map, tag, geometry, style, visible)
        is Polygon -> polygonManager.set(map, tag, geometry, style, visible)
      }
    }

  fun onCameraIdle() {
    clusterManager.onCameraIdle()
  }
}
