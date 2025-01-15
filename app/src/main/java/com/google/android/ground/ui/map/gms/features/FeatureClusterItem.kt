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

import com.google.android.gms.maps.model.LatLng
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.toGoogleMapsObject
import com.google.maps.android.clustering.ClusterItem

/** A [ClusterItem] implementation for clustering map [Feature]s. */
data class FeatureClusterItem(val feature: Feature) : ClusterItem {
  override fun getPosition(): LatLng = feature.geometry.center().toGoogleMapsObject()

  override fun getTitle(): String? = null

  override fun getSnippet(): String? = null

  override fun getZIndex(): Float? = null
}
