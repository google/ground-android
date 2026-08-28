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
import androidx.annotation.VisibleForTesting
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.algo.ScreenBasedAlgorithm
import com.google.maps.android.collections.MarkerManager
import org.groundplatform.android.R
import org.groundplatform.android.ui.IconFactory
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.toGoogleMapsObject
import org.groundplatform.domain.model.map.Bounds

/** Manages clustering of map [Feature]s. */
class FeatureClusterManager(
  private val context: Context,
  private val map: GoogleMap,
  markerManager: MarkerManager,
) {
  @VisibleForTesting
  val gmsClusterManager = ClusterManager<FeatureClusterItem>(context, map, markerManager)

  private val itemsByTag = mutableMapOf<Feature.Tag, FeatureClusterItem>()
  private val viewportPadding: Int by lazy {
    context.resources.getDimension(R.dimen.zoom_on_cluster_click_padding).toInt()
  }

  init {
    gmsClusterManager.setOnClusterClickListener(this::onClusterClick)
  }

  fun setAlgorithm(algorithm: ScreenBasedAlgorithm<FeatureClusterItem>) {
    gmsClusterManager.setAlgorithm(algorithm)
  }

  fun createRenderer(
    zoom: Float,
    iconFactory: IconFactory,
    onClusterRendered: (Feature.Tag) -> Unit,
    onClusterItemRendered: (Feature.Tag) -> Unit,
  ): FeatureClusterRenderer {
    val renderer = FeatureClusterRenderer(context, map, gmsClusterManager, zoom, iconFactory)
    renderer.onClusterRendered = onClusterRendered
    renderer.onClusterItemRendered = onClusterItemRendered
    gmsClusterManager.renderer = renderer
    return renderer
  }

  /** Adds the specified feature for clustering. */
  fun addFeature(feature: Feature) {
    removeFeature(feature.tag)
    val item = FeatureClusterItem(feature)
    gmsClusterManager.addItem(item)
    itemsByTag[feature.tag] = item
  }

  /** Removes the specified feature . */
  fun removeFeature(tag: Feature.Tag) {
    itemsByTag.remove(tag)?.let { gmsClusterManager.removeItem(it) }
  }

  /** Recomputes clusters and re-renders the map. */
  fun cluster() = gmsClusterManager.cluster()

  /** Notifies the underlying manager that the map camera has come to rest. */
  fun onCameraIdle() = gmsClusterManager.onCameraIdle()

  /** Pan and zoom the camera to the bounds of features contained in the selected cluster. */
  private fun onClusterClick(cluster: Cluster<FeatureClusterItem>): Boolean {
    Bounds.fromGeometries(cluster.items.map { it.feature.geometry })?.let { animateCamera(it) }
    return true
  }

  /** Center and zoom the viewport to the specified bounds, minus additional padding. */
  private fun animateCamera(bounds: Bounds) {
    map.animateCamera(
      CameraUpdateFactory.newLatLngBounds(bounds.toGoogleMapsObject(), viewportPadding)
    )
  }
}
