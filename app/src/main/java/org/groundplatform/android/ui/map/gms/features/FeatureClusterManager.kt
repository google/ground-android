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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.collections.MarkerManager
import org.groundplatform.android.R
import org.groundplatform.android.model.map.Bounds
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.GmsExt.toBounds
import org.groundplatform.android.ui.map.gms.toGoogleMapsObject

/** Manages clustering of map [Feature]s. */
class FeatureClusterManager(
  context: Context,
  private val map: GoogleMap,
  markerManager: MarkerManager,
) : ClusterManager<FeatureClusterItem>(context, map, markerManager) {

  private val itemsByTag = mutableMapOf<Feature.Tag, FeatureClusterItem>()
  private val viewportPadding: Int by lazy {
    context.resources.getDimension(R.dimen.zoom_on_cluster_click_padding).toInt()
  }

  init {
    setOnClusterClickListener(this::onClusterClick)
  }

  /** Adds the specified feature for clustering. */
  fun addFeature(feature: Feature) {
    removeFeature(feature.tag)
    val item = FeatureClusterItem(feature)
    addItem(item)
    itemsByTag[feature.tag] = item
  }

  /** Removes the specified feature . */
  fun removeFeature(tag: Feature.Tag) {
    itemsByTag.remove(tag)?.let { removeItem(it) }
  }

  /** Pan and zoom the camera to the bounds of features contained in the selected cluster. */
  private fun onClusterClick(cluster: Cluster<FeatureClusterItem>): Boolean {
    cluster.items.map { it.feature.geometry }.toBounds()?.let { animateCamera(it) }
    return true
  }

  /** Center and zoom the viewport to the specified bounds, minus additional padding. */
  private fun animateCamera(bounds: Bounds) {
    map.animateCamera(
      CameraUpdateFactory.newLatLngBounds(bounds.toGoogleMapsObject(), viewportPadding)
    )
  }
}
