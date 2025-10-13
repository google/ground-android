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

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon as MapsPolygon
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.PolyUtil.containsLocation
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.model.geometry.MultiPolygon
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.ui.map.Feature

/** Manages [Feature]s displayed on the map as Maps SDK items (marker, polyline, etc). */
class MapsItemManager(
  private val map: GoogleMap,
  private val pointRenderer: PointRenderer,
  private val polygonRenderer: PolygonRenderer,
  private val lineStringRenderer: LineStringRenderer,
) {
  private val itemsByTag = mutableMapOf<Feature.Tag, List<Any>>()

  /**
   * Adds one or more items to the map representing the specified [Feature], replacing and existing
   * items associated with same tag are replaced.
   */
  fun put(feature: Feature, visible: Boolean) =
    with(feature) {
      // If map item with this tag already exists, remove it.
      remove(tag)
      // Add item to map and index.
      itemsByTag[tag] =
        when (geometry) {
          is Point -> listOf(pointRenderer.add(map, tag, geometry, style, selected, visible))
          is Polygon -> listOf(polygonRenderer.add(map, tag, geometry, style, selected, visible))
          is MultiPolygon ->
            geometry.polygons.map { polygonRenderer.add(map, tag, it, style, selected, visible) }
          is LineString ->
            listOf(
              lineStringRenderer.add(map, tag, geometry, style, selected, visible, tooltipText)
            )
          else -> error("Render ${geometry.javaClass} geometry not supported")
        }
    }

  /** Removes map items associated with the specified feature's tag. */
  fun remove(tag: Feature.Tag) =
    itemsByTag.remove(tag)?.forEach {
      when (it) {
        is Marker -> it.remove()
        is Polyline -> it.remove()
        is MapsPolygon -> it.remove()
        else -> error("${it.javaClass.simpleName} map item not supported")
      }
    }

  /** Updates an already-rendered feature with new geometry and style. */
  fun update(feature: Feature) =
    with(feature) {
      itemsByTag[feature.tag]?.forEach {
        if (it is Polyline) {
          lineStringRenderer.update(map, it, geometry as LineString, tooltipText)
        } else {
          error("Unsupported map feature: ${it::class.java}")
        }
      }
    }

  /**
   * Shows or hides the items associated with the specified tag. Does nothing if there are no items
   * with that tag present.
   */
  fun setVisible(tag: Feature.Tag, visible: Boolean) =
    itemsByTag[tag]?.forEach {
      when (it) {
        is Marker -> it.isVisible = visible
        is Polyline -> it.isVisible = visible
        is MapsPolygon -> it.isVisible = visible
        else -> error("${it.javaClass.simpleName} map item not supported")
      }
    }

  /**
   * Returns the feature tags associated with of polygon map items which overlap with the specified
   * coordinates.
   */
  fun getIntersectingPolygonTags(latLng: LatLng): Set<Feature.Tag> {
    val flattenedItems = itemsByTag.values.flatten()
    val polygons = flattenedItems.filterIsInstance<MapsPolygon>()
    return polygons
      .filter { containsLocation(latLng, it.points, false) }
      .filter { it.holes.none { hole -> containsLocation(latLng, hole, false) } }
      .map { it.tag as Feature.Tag }
      .toSet()
  }
}
