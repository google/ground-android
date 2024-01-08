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

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon as MapsPolygon
import com.google.android.gms.maps.model.Polyline
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.map.Feature
import com.google.maps.android.PolyUtil

class MapsItemManager(
  private val map: GoogleMap,
  private val pointRenderer: PointRenderer,
  private val polygonRenderer: PolygonRenderer,
  private val lineStringRenderer: LineStringRenderer
) {
  private val itemsByTag = mutableMapOf<Feature.Tag, Array<Any>>()

  fun add(feature: Feature, visible: Boolean) =
    with(feature) {
      // If map item with this tag already exists, remove it.
      remove(tag)
      // Add item to map and index.
      itemsByTag[tag] =
        when (geometry) {
          // TODO(!!!) Inject Map in Renderers as well
          is Point -> arrayOf(pointRenderer.add(map, tag, geometry, style, visible))
          is Polygon -> arrayOf(polygonRenderer.add(map, tag, geometry, style, visible))
          is MultiPolygon ->
            geometry.polygons
              .map { polygonRenderer.add(map, tag, it, style, visible) }
              .toTypedArray()
          is LineString -> arrayOf(lineStringRenderer.add(map, tag, geometry, style, visible))
          else -> error("Render ${geometry.javaClass} geometry not supported")
        }
    }

  fun remove(tag: Feature.Tag) =
    itemsByTag.remove(tag)?.forEach {
      when (it) {
        is Marker -> it.remove()
        is Polyline -> it.remove()
        is MapsPolygon -> it.remove()
        else -> error("${it.javaClass.simpleName} map item not supported")
      }
    }

  fun setVisible(tag: Feature.Tag, visible: Boolean) =
    itemsByTag[tag]?.forEach {
      when (it) {
        is Marker -> it.isVisible = visible
        is Polyline -> it.isVisible = visible
        is MapsPolygon -> it.isVisible = visible
        else -> error("${it.javaClass.simpleName} map item not supported")
      }
    }

  fun getIntersectingPolygonTags(latLng: LatLng): Set<Feature.Tag> {
    // TODO(!!!): Ignore holes!
    val flattenedItems = itemsByTag.values.flatMap { it.toList() }
    val polygons = flattenedItems.filterIsInstance<MapsPolygon>()
    return polygons
      .filter { PolyUtil.containsLocation(latLng, it.points, false) }
      .map { it.tag as Feature.Tag }
      .toSet()
  }
}
