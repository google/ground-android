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
package org.groundplatform.android.ui.map.gms.features

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import org.groundplatform.android.Config.CLUSTERING_ZOOM_THRESHOLD
import org.groundplatform.android.ui.IconFactory
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.CLUSTER_Z

/**
 * A cluster renderer for [FeatureClusterItem]s.
 *
 * Cluster rendering is determined by checking the current map zoom level against a given threshold.
 * Cluster bubbles are shown when the zoom level is less than [CLUSTERING_ZOOM_THRESHOLD], otherwise
 * they are hidden. Individual clustered map items are not rendered by this class. Instead, callers
 * should hide and show items in callbacks set in [onClusterRendered] and [onClusterItemRendered],
 * respectively.
 */
class FeatureClusterRenderer(
  context: Context,
  map: GoogleMap,
  clusterManager: ClusterManager<FeatureClusterItem>,
  var zoom: Float,
) : DefaultClusterRenderer<FeatureClusterItem>(context, map, clusterManager) {
  /**
   * Called when the cluster balloon is shown so that implementations can unhide related map items.
   */
  lateinit var onClusterRendered: (Feature.Tag) -> Unit
  /**
   * Called when the cluster balloon is display so that implementations can hide related map items.
   */
  lateinit var onClusterItemRendered: (Feature.Tag) -> Unit

  private val markerIconFactory: IconFactory = IconFactory(context)

  /**
   * Hides the marker provided by [DefaultClusterRenderer] and instead triggers the callback
   * provided in [onClusterItemRendered]. Called when zooming out past [CLUSTERING_ZOOM_THRESHOLD].
   */
  override fun onBeforeClusterItemRendered(item: FeatureClusterItem, markerOptions: MarkerOptions) {
    markerOptions.visible(false)
    onClusterItemRendered(item.feature.tag)
  }

  /**
   * Creates an icon with a label indicating the number of features with [flag] set over the total
   * number of features in the cluster.
   */
  private fun createClusterIcon(cluster: Cluster<FeatureClusterItem>): BitmapDescriptor {
    val itemsWithFlag = cluster.items.count { it.feature.flag }
    val totalItems = cluster.items.size
    return markerIconFactory.getClusterIcon("$itemsWithFlag/$totalItems")
  }

  /**
   * Triggers the callback provided in [onClusterRendered] to hide the individual map items and
   * renders the app's custom cluster balloon. Called when zooming in to [CLUSTERING_ZOOM_THRESHOLD]
   * .
   */
  override fun onBeforeClusterRendered(
    cluster: Cluster<FeatureClusterItem>,
    markerOptions: MarkerOptions,
  ) {
    // Hide cluster's items when clustered.
    cluster.items.forEach { onClusterRendered(it.feature.tag) }

    super.onBeforeClusterRendered(cluster, markerOptions)

    with(markerOptions) {
      icon(createClusterIcon(cluster))
      zIndex(CLUSTER_Z)
    }
  }

  /** Refreshes the counts shown on the cluster icon when reclustering, including on zoom. */
  override fun onClusterUpdated(cluster: Cluster<FeatureClusterItem>, marker: Marker) {
    marker.setIcon(createClusterIcon(cluster))
  }

  /**
   * Indicates whether or not a cluster should be rendered as a cluster or individual map items.
   *
   * Returns true iff the current zoom level is less than [CLUSTERING_ZOOM_THRESHOLD].
   */
  override fun shouldRenderAsCluster(cluster: Cluster<FeatureClusterItem>): Boolean =
    zoom < CLUSTERING_ZOOM_THRESHOLD
}
