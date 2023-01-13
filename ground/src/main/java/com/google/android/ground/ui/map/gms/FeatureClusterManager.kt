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
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.util.toImmutableSet
import com.google.maps.android.clustering.ClusterManager
import timber.log.Timber

/** Manages clusters of map [Feature]s. */
class FeatureClusterManager(context: Context?, map: GoogleMap) :
  ClusterManager<FeatureClusterItem>(context, map) {
  var activeLocationOfInterest: String? = null

  /** Manage a given map feature and add it to an appropriate cluster. */
  fun addOrUpdateLocationOfInterestFeature(feature: Feature) {
    // TODO(#1152): Add support for polygons.
    if (feature.geometry !is Point) {
      Timber.d("can't manage a non-point")
      return
    }

    when (feature.tag) {
      Feature.Type.LOCATION_OF_INTEREST -> {
        val clusterItem = algorithm.items.find { it.feature.id == feature.id }

        if (clusterItem != null) {
          updateItem(clusterItem)
        } else {
          Timber.d("adding loi to cluster manager: ${feature}")
          addItem(FeatureClusterItem(feature))
        }
      }
      else -> {}
    }
  }

  /** Remove a set of features from this manager's clusters. */
  fun removeLocationOfInterestFeatures(features: Set<Feature>) {
    val newIds = features.map { it.id }.toSet()
    val missingItems = algorithm.items.filter { !newIds.contains(it.feature.id) }.toSet()

    Timber.d("removing points: $missingItems")
    removeItems(missingItems)
  }

  /** Returns all of the map [Feature]s currently managed by this cluster manager. */
  fun getManagedFeatures() = algorithm.items.map { it.feature }.toImmutableSet()
}
