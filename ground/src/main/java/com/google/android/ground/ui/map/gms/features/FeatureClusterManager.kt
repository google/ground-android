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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.ground.R
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.GmsExt.toBounds
import com.google.android.ground.ui.map.gms.toGoogleMapsObject
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager

/** Manages clusters of map [Feature]s. */
class FeatureClusterManager(context: Context, private val map: GoogleMap) :
  ClusterManager<FeatureClusterItem>(context, map) {

  private val itemsByTag = mutableMapOf<Feature.Tag, FeatureClusterItem>()
  private val zoomOnClickPadding: Int by lazy {
    context.resources.getDimension(R.dimen.zoom_on_cluster_click_padding).toInt()
  }

  init {
    setOnClusterClickListener(this::onClusterClick)
  }

  fun addFeature(feature: Feature) {
    val item = FeatureClusterItem(feature)
    addItem(item)
    itemsByTag[feature.tag] = item
  }

  fun removeFeature(tag: Feature.Tag) {
    itemsByTag.remove(tag)?.let { removeItem(it) }
  }

  /** Pan and zoom the camera to the bounds of contained LOIs. */
  private fun onClusterClick(cluster: Cluster<FeatureClusterItem>): Boolean {
    cluster.items.map { it.feature.geometry }.toBounds()?.let { moveCamera(it) }
    return true
  }

  private fun moveCamera(bounds: Bounds) {
    map.animateCamera(
      CameraUpdateFactory.newLatLngBounds(bounds.toGoogleMapsObject(), zoomOnClickPadding)
    )
  }
}
