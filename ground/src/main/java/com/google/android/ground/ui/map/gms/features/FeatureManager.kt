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
import com.google.android.ground.Config
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.FeatureClusterItem
import com.google.android.ground.ui.map.gms.FeatureClusterRenderer
import com.google.maps.android.PolyUtil
import com.google.maps.android.clustering.ClusterManager
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
  private lateinit var clusterManager: ClusterManager<FeatureClusterItem>

  fun onMapReady(map: GoogleMap) {
    this.map = map
    clusterManager = ClusterManager<FeatureClusterItem>(context, map)
    val clusterRenderer =
      FeatureClusterRenderer(
        context,
        map,
        clusterManager,
        this::showItem,
        this::hideItem,
        Config.CLUSTERING_ZOOM_THRESHOLD,
        map.cameraPosition.zoom
      )
    //    clusterManager.setOnClusterClickListener(this::onClusterItemClick) // TODO(!!!): Add
    // callback
    clusterManager.renderer = clusterRenderer
  }

  private fun hideItem(tag: Feature.Tag) {
    TODO("Not yet implemented")
  }

  private fun showItem(tag: Feature.Tag) {
    TODO("Not yet implemented")
  }

  fun setFeatures(updatedFeatures: Collection<Feature>) {
    // remove stale
    val removedOrChanged = features - updatedFeatures.toSet()
    removedOrChanged.forEach(this::removeFeature)
    // add missing
    val newOrChanged = updatedFeatures - features
    newOrChanged.forEach { addFeature(it) }
  }

  private fun addFeature(feature: Feature) {
    with(feature) {
      features.add(this)
      featuresByTag[tag] = this
      when (geometry) {
        is Point -> pointManager.set(map, tag, geometry, style, visible = clusterable)
        is LineString -> lineStringManager.set(map, tag, geometry, style, visible = clusterable)
        is LinearRing -> error("LinearRing rendering not supported")
        is MultiPolygon -> multiPolygonManager.set(map, tag, geometry, style, visible = clusterable)
        is Polygon -> polygonManager.set(map, tag, geometry, style, visible = clusterable)
      }
    }
  }

  fun removeFeature(feature: Feature) {
    // Remove from all managers in case geometry type changed.
    mapItemManagers.forEach { it.remove(feature.tag) }
    features.remove(feature)
    featuresByTag.remove(feature.tag)
  }

  fun getIntersectingPolygons(latLng: LatLng): Set<Feature> {
    val polygons = polygonManager.items + multiPolygonManager.items.flatten()
    return polygons
      .filter { PolyUtil.containsLocation(latLng, it.points, false) }
      .mapNotNull { featuresByTag[it.tag as Feature.Tag] }
      .toSet()
  }
}
