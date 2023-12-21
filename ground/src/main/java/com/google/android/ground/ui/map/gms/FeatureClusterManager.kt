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
import com.google.android.ground.ui.map.Feature
import com.google.maps.android.clustering.ClusterManager
import kotlinx.collections.immutable.toPersistentSet
import timber.log.Timber

/** Manages clusters of map [Feature]s. */
class FeatureClusterManager(context: Context?, map: GoogleMap) :
  ClusterManager<FeatureClusterItem>(context, map) {
  var activeLocationOfInterest: String? = null

  /** Manage a given map feature and add it to an appropriate cluster. */
  fun addFeature(feature: Feature) {
    val clusterItem = algorithm.items.find { it.feature.tag.id == feature.tag.id }
    if (clusterItem != null) {
      updateItem(clusterItem)
    } else {
      Timber.v("adding loi to cluster manager: $feature")
      addItem(FeatureClusterItem(feature))
    }
  }

  /** Removes stale features from this manager's clusters. */
  fun removeStaleFeatures(features: Set<Feature>) {
    val deletedIds = algorithm.items.map { it.feature.tag.id } - features.map { it.tag.id }.toSet()
    val deletedFeatures = algorithm.items.filter { deletedIds.contains(it.feature.tag.id) }

    Timber.v("removing points: $deletedFeatures")
    removeItems(deletedFeatures)
  }

  /** Removes all features from this manager's clusters. */
  fun removeAllFeatures() {
    // TODO(#1895): Move this method to GoogleMapsFragment.
    val deletedFeatures = algorithm.items

    Timber.v("removing points: $deletedFeatures")
    removeItems(deletedFeatures)
  }

  /** Returns all of the map [Feature]s currently managed by this cluster manager. */
  // TODO(#1895): Move this method to GoogleMapsFragment.
  fun getManagedFeatures() = algorithm.items.map { it.feature }.toPersistentSet()
}
