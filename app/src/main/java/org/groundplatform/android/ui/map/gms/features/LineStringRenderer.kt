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
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import javax.inject.Inject
import org.groundplatform.android.R
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.POLYLINE_Z
import org.groundplatform.android.ui.map.gms.toLatLngList
import org.groundplatform.android.ui.util.BitmapUtil

class LineStringRenderer
@Inject
constructor(private val resources: Resources, private val bitmapUtil: BitmapUtil) :
  MapsItemRenderer<LineString, Polyline> {

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
  ): Polyline {
    val options = PolylineOptions()
    with(options) {
      clickable(false)
      addAll(geometry.coordinates.toLatLngList())
      visible(visible)
    }
    val polyline = map.addPolyline(options)
    polyline.tag = tag
    with(polyline) {
      if (style.vertexStyle == Feature.VertexStyle.CIRCLE) {
        startCap = circleCap
        endCap = circleCap
      }

      val strokeScale = if (selected) 2f else 1f
      width = defaultStrokeWidth * strokeScale
      color = style.color
      jointType = JointType.ROUND
      zIndex = POLYLINE_Z
    }
    return polyline
  }
}
