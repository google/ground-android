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

import android.content.res.Resources
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import javax.inject.Inject
import org.groundplatform.android.R
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.POLYLINE_Z
import org.groundplatform.android.ui.map.gms.toLatLngList
import org.groundplatform.android.ui.util.BitmapUtil
import org.groundplatform.android.util.midpoint
import org.groundplatform.android.util.penult

class LineStringRenderer
@Inject
constructor(
  private val resources: Resources,
  private val bitmapUtil: BitmapUtil,
  private val tooltipMarkerRenderer: TooltipMarkerRenderer,
) : MapsItemRenderer<LineString, Polyline> {

  // These must be done lazily since resources are not available before the app completes
  // initialization.
  private val defaultStrokeWidth by lazy { resources.getDimension(R.dimen.line_geometry_width) }
  private val circleCap by lazy {
    val bitmap = bitmapUtil.fromVector(R.drawable.ic_circle_marker)
    CustomCap(BitmapDescriptorFactory.fromBitmap(bitmap))
  }

  override fun add(
    map: GoogleMap,
    tag: Feature.Tag,
    geometry: LineString,
    style: Feature.Style,
    selected: Boolean,
    visible: Boolean,
    tooltipText: String?,
  ): Polyline {
    val options = PolylineOptions().clickable(false)
    options.addAll(convertMaybeAddOffsetPoint(geometry))
    options.visible(visible)

    val polyline =
      map.addPolyline(options).apply {
        this.tag = tag
        applyStyle(style, selected)
      }

    updateTooltipMarker(geometry, map, tooltipText)
    return polyline
  }

  override fun update(
    map: GoogleMap,
    mapFeature: Polyline,
    geometry: LineString,
    tooltipText: String?,
  ) {
    mapFeature.points = convertMaybeAddOffsetPoint(geometry)

    // TODO: Move tooltip rendering out of the LineStringRenderer.
    updateTooltipMarker(geometry, map, tooltipText)
  }

  private fun updateTooltipMarker(geometry: LineString, map: GoogleMap, tooltipText: String?) {
    val coords = geometry.coordinates
    val midpoint = if (coords.size < 2) null else coords.last().midpoint(coords.penult())
    tooltipMarkerRenderer.show(map, midpoint, tooltipText)
  }

  private fun Polyline.applyStyle(style: Feature.Style, selected: Boolean) {
    if (style.vertexStyle == Feature.VertexStyle.CIRCLE) {
      startCap = circleCap
      endCap = circleCap
    }
    width = calculateStrokeWidth(selected)
    color = style.color
    jointType = JointType.ROUND
    zIndex = POLYLINE_Z
    isVisible = true
  }

  private fun calculateStrokeWidth(selected: Boolean): Float {
    val strokeScale = if (selected) 2f else 1f
    return defaultStrokeWidth * strokeScale
  }

  private fun convertMaybeAddOffsetPoint(geometry: LineString): List<LatLng> {
    val coordinates = geometry.coordinates.toLatLngList()
    // A polyline becomes invalid only when there are exactly two identical points.
    if (coordinates.size == 2 && coordinates[0] == coordinates[1]) {
      return listOf(coordinates[0], coordinates[1].addOffset())
    }

    return coordinates
  }

  /**
   * Adds a tiny offset to the [LatLng] to ensure that the polyline has a non-zero length. This is
   * required for the start/end caps to be rendered.
   */
  private fun LatLng.addOffset(): LatLng = LatLng(latitude + 1e-6, longitude + 1e-6)
}
