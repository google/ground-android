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
import org.groundplatform.android.model.geometry.LineString
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
   * Tracks the current "in-progress" user-drawn line (draft). This ensures subsequent updates to
   * the draft re-use the same polyline instance instead of constantly removing/re-adding it.
   */
  private var activeDraftTag: Feature.Tag? = null
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
        val tag = marker.tag as Feature.Tag
        val feature = featuresByTag[tag] ?: error("Feature not found for tag: $tag")
        coroutineScope.launch { _markerClicks.emit(feature) }
        return true
      }
    }

  /**
   * Syncs the map state with the provided set of [Feature]s.
   *
   * Behavior:
   * - If there is an **active draft line** already on the map (tracked via [activeDraftTag]), and a
   *   new draft `Feature` comes in, we update its polyline geometry and style **in place** using
   *   [updateLineString] instead of removing/re-adding. This keeps the line smooth while dragging.
   * - If no draft is currently active but a new one arrives, we "adopt" its tag as the active
   *   draft.
   * - If no draft is present in [updatedFeatures], we clear [activeDraftTag].
   * - All non-draft features are reconciled using the standard "diff": remove stale, add new.
   *
   * Clustering:
   * - Clustering is always refreshed after applying the diff. For draft updates we skip diffing
   *   entirely and just update the geometry directly.
   */
  fun setFeatures(updatedFeatures: Collection<Feature>) {
    val incomingDraft = updatedFeatures.firstOrNull { isDraftLineString(it) }

    // Case 1: We already have a draft rendered and got another draft update → update in place.
    if (activeDraftTag != null && incomingDraft != null) {
      val oldTag = activeDraftTag!!
      val ls = incomingDraft.geometry as? LineString
      if (ls != null && featuresByTag.containsKey(oldTag)) {
        updateLineString(
          oldTag,
          ls,
          incomingDraft.style,
          incomingDraft.selected,
          incomingDraft.tooltipText,
        )
        Timber.d("Updated draft line in place for tag=$oldTag")
        return
      }
    }

    // Case 2: No active draft yet, but a draft arrived → adopt it.
    if (activeDraftTag == null && incomingDraft != null) {
      activeDraftTag = incomingDraft.tag
    }

    // Case 3: No draft in the update set → clear our pointer.
    if (incomingDraft == null) {
      activeDraftTag = null
    }

    // Default path: reconcile all non-draft features.
    val updatedSet = updatedFeatures.toSet()
    val removedOrChanged = features - updatedSet
    removedOrChanged.forEach(this::remove)

    val newOrChanged = updatedSet - features
    newOrChanged.forEach(this::add)

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

  fun onCameraIdle() {
    clusterManager.onCameraIdle()
  }

  /** Helper to detect whether a [Feature] is a draft line that should be updated in place. */
  private fun isDraftLineString(feature: Feature): Boolean =
    feature.geometry is LineString &&
      !feature.clusterable &&
      feature.selected &&
      feature.tag.type == Feature.Type.USER_POLYGON

  /**
   * Updates the geometry + style of an existing polyline on the map in place.
   *
   * This is used primarily for draft lines during dragging, where vertices are updated
   * continuously. The map item stays the same; only its points/style change.
   *
   * Also keeps [features] and [featuresByTag] in sync so future diffing sees the updated state.
   */
  fun updateLineString(
    tag: Feature.Tag,
    geometry: LineString,
    style: Feature.Style,
    selected: Boolean,
    tooltipText: String?,
  ) {
    coroutineScope.launch {
      mapsItemManager.updateLineString(tag, geometry, style, selected, tooltipText)

      // keep local indices consistent
      featuresByTag[tag]?.let { prev ->
        val updated =
          prev.copy(
            geometry = geometry,
            style = style,
            selected = selected,
            tooltipText = tooltipText ?: prev.tooltipText,
          )
        features.remove(prev)
        features.add(updated)
        featuresByTag[tag] = updated
      }
    }
  }
}
