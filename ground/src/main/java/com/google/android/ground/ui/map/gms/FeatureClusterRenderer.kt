/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.map.gms

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.IconFactory
import com.google.android.ground.ui.map.gms.renderer.PointFeatureManager
import com.google.android.ground.ui.map.gms.renderer.PolygonFeatureManager
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.view.DefaultClusterRenderer

/**
 * A cluster renderer for [FeatureClusterItem]s.
 *
 * Cluster rendering is determined by checking the current map zoom level against a given threshold.
 * Clusters will render when the zoom level is lesser than the threshold, otherwise we render
 * individual markers for each cluster item.
 */
class FeatureClusterRenderer(
  context: Context,
  map: GoogleMap,
  clusterManager: FeatureClusterManager,
  private val pointFeatureManager: PointFeatureManager,
  private val polygonFeatureManager: PolygonFeatureManager,
  private val clusteringZoomThreshold: Float,
  /**
   * The current zoom level to compare against the renderer's threshold.
   *
   * To use the current zoom level of the map, this value must be updated on the main thread. Do not
   * attempt to use the map instance initially passed to the renderer, as renderer methods may not
   * run on the main thread.
   */
  var zoom: Float
) : DefaultClusterRenderer<FeatureClusterItem>(context, map, clusterManager) {

  private val markerIconFactory: IconFactory = IconFactory(context)

  /** Sets appropriate styling for clustered items prior to rendering. */
  override fun onBeforeClusterItemRendered(item: FeatureClusterItem, markerOptions: MarkerOptions) {
    val style = item.feature.style
    when (item.feature.geometry) {
      is Point -> {
        // Cluster manager actually manages points.
        // TODO: Can we delegate this to PointFeatureManager as we do for polygonFeatureManager?
        pointFeatureManager.setMarkerOptions(markerOptions, style)
      }
      is Polygon,
      is MultiPolygon -> {
        // Don't render marker if this item is a polygon.
        markerOptions.visible(false)
        // Add polygon or multi-polygon when zooming in.
        polygonFeatureManager.putFeature(item.feature)
      }
      else -> {
        throw UnsupportedOperationException(
          "Unsupported feature type ${item.feature.geometry.javaClass.simpleName}"
        )
      }
    }
  }

  override fun onClusterItemUpdated(item: FeatureClusterItem, marker: Marker) {
    val feature = item.feature
    when (feature.geometry) {
      is Point -> marker.setIcon(pointFeatureManager.getMarkerIcon(feature.style))
      is Polygon,
      is MultiPolygon ->
        // Update polygon or multi-polygon on change.
        polygonFeatureManager.updateFeature(feature)
      else ->
        throw UnsupportedOperationException(
          "Unsupported feature geometry type ${feature.geometry.javaClass.simpleName}"
        )
    }
    super.onClusterItemUpdated(item, marker)
  }

  /**
   * Creates an icon with a label indicating the number of features with [flag] set over the total
   * number of features in the cluster.
   */
  private fun createClusterIcon(cluster: Cluster<FeatureClusterItem>): BitmapDescriptor {
    val itemsWithFlag = cluster.items.count { it.feature.tag.flag }
    val totalItems = cluster.items.size
    return markerIconFactory.getClusterIcon("$itemsWithFlag/$totalItems")
  }

  override fun onBeforeClusterRendered(
    cluster: Cluster<FeatureClusterItem>,
    markerOptions: MarkerOptions
  ) {
    // Remove non-points before rendering cluster.
    cluster.items
      .map { it.feature }
      .filter { it.geometry !is Point }
      .forEach { feature -> polygonFeatureManager.removeFeature(feature) }

    super.onBeforeClusterRendered(cluster, markerOptions)

    with(markerOptions) {
      icon(createClusterIcon(cluster))
      zIndex(CLUSTER_Z)
    }
  }

  override fun onClusterUpdated(cluster: Cluster<FeatureClusterItem>, marker: Marker) {
    super.onClusterUpdated(cluster, marker)
    marker.setIcon(createClusterIcon(cluster))
  }

  /**
   * Indicates whether or not a cluster should be rendered as a cluster or individual markers.
   *
   * Only true when the current zoom level is lesser than a set threshold.
   */
  override fun shouldRenderAsCluster(cluster: Cluster<FeatureClusterItem>): Boolean =
    zoom < clusteringZoomThreshold
}
