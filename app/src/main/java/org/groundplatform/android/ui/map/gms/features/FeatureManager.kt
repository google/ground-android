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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.collections.MarkerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.groundplatform.android.coroutines.MainScope
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
  private lateinit var clusterManager: FeatureClusterManager
  private lateinit var clusterRenderer: FeatureClusterRenderer

  private val _markerClicks: MutableSharedFlow<Feature> = MutableSharedFlow()
  val markerClicks = _markerClicks.asSharedFlow()

  /**
   * The camera's current zoom level. This must be set here since this impl can't access
   * `map.cameraPosition` from off the main UI thread.
   */
  var zoom: Float
    get() = clusterRenderer.zoom
    set(value) {
      clusterRenderer.zoom = value
    }

  /** Clears all managed state an binds to the provided [GoogleMap]. */
  fun onMapReady(map: GoogleMap) {
    features.clear()
    featuresByTag.clear()
    mapsItemManager = MapsItemManager(map, pointRenderer, polygonRenderer, lineStringRenderer)
    clusterManager = FeatureClusterManager(context, map, createMarkerManager(map))
    clusterRenderer = FeatureClusterRenderer(context, map, clusterManager, map.cameraPosition.zoom)
    clusterRenderer.onClusterItemRendered = { mapsItemManager.setVisible(it, true) }
    clusterRenderer.onClusterRendered = { mapsItemManager.setVisible(it, false) }
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
   * Adds a feature to the map, cluster, and to this class' internal index. Clusterable features are
   * initialized as hidden so that the clusterer can determine whether they should be shown based on
   * zoom level.
   */
  private fun add(feature: Feature) =
    with(feature) {
      features.add(this)
      featuresByTag[tag] = this
      if (clusterable) clusterManager.addFeature(this)
      mapsItemManager.put(this, visible = !clusterable)
    }

  private fun remove(feature: Feature) =
    with(feature) {
      features.remove(this)
      featuresByTag.remove(tag)
      mapsItemManager.remove(tag)
      clusterManager.removeFeature(tag)
    }

  /** Updates the existing feature on the map with it's new properties (geometry, styling, etc). */
  fun update(feature: Feature) =
    with(feature) {
      val prevFeature = featuresByTag[tag]
      if (prevFeature == null) {
        Timber.e("Feature not found for update: $tag")
        return
      }

      features.remove(prevFeature)
      features.add(this)
      mapsItemManager.update(this)
      featuresByTag[tag] = this
    }

  fun onCameraIdle() {
    clusterManager.onCameraIdle()
  }
}
