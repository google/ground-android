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

/** Manages clusters of map [Feature]s. */
class FeatureClusterManager(context: Context?, map: GoogleMap) :
  ClusterManager<FeatureClusterItem>(context, map) {
  /** Returns all of the map [Feature]s currently managed by this cluster manager. */
  // TODO(#1895): Move this method to GoogleMapsFragment.
  val features: Set<Feature>
    get() = algorithm.items.map { it.feature }.toSet()

  var activeLocationOfInterest: String? = null

  fun setFeatures(newFeatures: Set<Feature>) {
    val newClusterFeatures = newFeatures.filter { it.clusterable }.toSet()
    val staleFeatures = features - newClusterFeatures
    removeFeatures(staleFeatures)
    val missingFeatures = newClusterFeatures - features
    missingFeatures.forEach(this::putFeature)
    cluster()
  }

  /** Adds the specified map feature if an identical feature is not already present. */
  private fun putFeature(feature: Feature) {
    if (algorithm.items.none { it.feature == feature }) {
      addItem(FeatureClusterItem(feature))
    }
  }

  /** Removes the specified features from the set of managed clustered features. */
  private fun removeFeatures(features: Set<Feature>) {
    removeItems(algorithm.items.filter { features.contains(it.feature) })
  }
}
