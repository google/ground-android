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

/** Manages clusters of location of interest map [Feature]s. */
class LocationOfInterestClusterManager(context: Context?, map: GoogleMap) :
  ClusterManager<LocationOfInterestClusterItem>(context, map) {
  var activeLocationOfInterest: String? = null

  /** Manage a given location of interest feature and add it to an appropriate cluster. */
  fun addOrUpdateLocationOfInterestFeature(feature: Feature) {
    if (feature.geometry !is Point) {
      // TODO(#1152): Add support for polygons.
      Timber.d("can't manage a non-point")
      return
    }

    when (feature.tag) {
      is Feature.LocationOfInterestTag -> {
        val clusterItem = algorithm.items.find { it.locationOfInterestId == feature.tag.id }

        if (clusterItem != null) {
          updateItem(clusterItem)
        } else {
          Timber.d("adding loi to cluster manager: ${feature}")
          addItem(
            LocationOfInterestClusterItem(
              feature.geometry,
              feature.tag.caption,
              feature.tag.lastModified,
              feature.tag.id,
            )
          )
        }
      }
      else -> {}
    }
  }

  /** Remove a set of features from this manager's clusters. */
  fun removeLocationOfInterestFeatures(features: Set<Feature>) {
    val existingIds = algorithm.items.map { it.locationOfInterestId }.toSet()
    val deletedIds = existingIds.intersect(features.map { it.tag.id }.toSet())
    val deletedPoints: Set<LocationOfInterestClusterItem> =
      algorithm.items.filter { deletedIds.contains(it.locationOfInterestId) }.toSet()

    Timber.d("removing points: ${deletedPoints}")
    removeItems(deletedPoints)
  }

  /**
   * Returns all of the location of interest map [Feature]s currently managed by this cluster
   * manager.
   */
  fun getManagedLocationOfInterestFeatures() =
    algorithm.items
      .map {
        Feature(
          Feature.LocationOfInterestTag(it.locationOfInterestId, it.title, it.snippet),
          it.position.toPoint()
        )
      }
      .toImmutableSet()
}
