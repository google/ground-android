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
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm
import com.google.maps.android.collections.MarkerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.groundplatform.android.di.coroutines.MainScope
import org.groundplatform.android.ui.map.Feature
import timber.log.Timber

/**
 * Coordinates adding, removing, and updating of [Feature]s to the map. This abstracts access to
 * both rendering of individual features, as well as clustering. This class is not thread-safe.
 */
class FeatureManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  @MainScope private val coroutineScope: CoroutineScope,
  private val pointRenderer: PointRenderer,
  private val polygonRenderer: PolygonRenderer,
  private val lineStringRenderer: LineStringRenderer,
) {
  private val features = mutableSetOf<Feature>()
  private val featuresByTag = mutableMapOf<Feature.Tag, Feature>()

  private lateinit var map: GoogleMap
  private lateinit var mapsItemManager: MapsItemManager
  @VisibleForTesting internal lateinit var clusterManager: FeatureClusterManager
  @VisibleForTesting internal lateinit var clusterRenderer: FeatureClusterRenderer

  private val _markerClicks: MutableSharedFlow<Feature> = MutableSharedFlow()
  val markerClicks = _markerClicks.asSharedFlow()

  /**
   * The camera's current zoom level. This must be set here since this impl can't access
   * `map.cameraPosition` from off the main UI thread.
   */
  fun setZoom(newValue: Float) {
    clusterRenderer.setZoom(newValue)
  }

  /** Clears all managed state an binds to the provided [GoogleMap]. */
  fun onMapReady(map: GoogleMap) {
    features.clear()
    featuresByTag.clear()
    mapsItemManager = MapsItemManager(map, pointRenderer, polygonRenderer, lineStringRenderer)
    clusterManager = FeatureClusterManager(context, map, createMarkerManager(map))
    // Render only visible features; off-screen clusterable features are omitted
    clusterManager.setAlgorithm(
      with(context.resources.displayMetrics) {
        NonHierarchicalViewBasedAlgorithm(
          (widthPixels / density).toInt(),
          (heightPixels / density).toInt(),
        )
      }
    )
    clusterRenderer = FeatureClusterRenderer(context, map, clusterManager, map.cameraPosition.zoom)
    clusterRenderer.onClusterItemRendered = { showClusterableItem(it) }
    clusterRenderer.onClusterRendered = { hideClusterableItem(it) }
    clusterManager.renderer = clusterRenderer
    this.map = map
  }

  private fun createMarkerManager(map: GoogleMap): MarkerManager =
    object : MarkerManager(map) {
      override fun onMarkerClick(marker: Marker): Boolean {
        if (super.onMarkerClick(marker)) return true
        val tag =
          marker.tag as? Feature.Tag
            ?: run {
              Timber.e("Invalid marker tag: ${marker.tag}")
              return false
            }
        val feature = featuresByTag[tag] ?: error("Feature not found for tag: $tag")
        coroutineScope.launch { _markerClicks.emit(feature) }
        return true
      }
    }

  /**
   * Updates the current set of features managed by the manager, adding and removing items from the
   * map as needed to sync the map state with the provided collection.
   */
  fun setFeatures(updatedFeatures: Collection<Feature>) {
    updatedFeatures.forEach { feature ->
      val existingFeature = featuresByTag[feature.tag]
      // A non-clustered feature whose tag already exists but whose contents changed can be moved in
      // place, instead of being removed and re-added (which flickers).
      val shouldUpdate =
        existingFeature != null && existingFeature != feature && !feature.clusterable
      if (shouldUpdate) update(existing = existingFeature, updated = feature)
    }
    // remove stale
    val removedOrChanged = features - updatedFeatures.toSet()
    removedOrChanged.forEach(this::remove)
    // add missing
    val newOrChanged = updatedFeatures - features
    newOrChanged.forEach(this::add)
    // cluster and update visibility
    clusterManager.cluster()
    Timber.v("${removedOrChanged.size} features removed, ${newOrChanged.size} added")
  }

  /**
   * Returns the set of areas (polygon or multi-polygon features) which overlap with the specified
   * coordinates.
   */
  fun getIntersectingPolygons(latLng: LatLng): Set<Feature> =
    mapsItemManager.getIntersectingPolygonTags(latLng).mapNotNull { featuresByTag[it] }.toSet()

  /**
   * Adds a feature to the cluster and to this class' internal index. Clusterable feature map items
   * are created only when drawn individually to reduce heap pressure.
   */
  private fun add(feature: Feature) =
    with(feature) {
      features.add(this)
      featuresByTag[tag] = this
      if (clusterable) {
        clusterManager.addFeature(this)
      } else {
        mapsItemManager.put(this, visible = true)
      }
    }

  /** Draws a clustered feature individually, creating its map item if it doesn't have one yet. */
  private fun showClusterableItem(tag: Feature.Tag) {
    if (mapsItemManager.contains(tag)) {
      mapsItemManager.setVisible(tag, true)
    } else {
      featuresByTag[tag]?.let { mapsItemManager.put(it, visible = true) }
    }
  }

  private fun hideClusterableItem(tag: Feature.Tag) {
    mapsItemManager.remove(tag)
  }

  private fun remove(feature: Feature) =
    with(feature) {
      features.remove(this)
      featuresByTag.remove(tag)
      mapsItemManager.remove(tag)
      clusterManager.removeFeature(tag)
    }

  /** Updates the existing feature on the map with it's new properties (geometry, styling, etc). */
  private fun update(existing: Feature, updated: Feature) {
    if (!mapsItemManager.update(updated)) return

    features.remove(existing)
    features.add(updated)
    featuresByTag[updated.tag] = updated
  }

  fun onCameraIdle() {
    clusterManager.onCameraIdle()
  }
}
