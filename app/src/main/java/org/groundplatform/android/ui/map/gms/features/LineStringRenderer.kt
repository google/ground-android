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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
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
  private var tooltipMarker: Marker? = null

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

    updateTooltipMarker(map, geometry)
    return polyline
  }

  private fun updateTooltipMarker(map: GoogleMap, geometry: LineString) {
    if (geometry.coordinates.size < 2) {
      removeTooltipMarker()
      return
    }

    val lastVertex = geometry.coordinates.last()
    val secondLastVertex = geometry.coordinates[geometry.coordinates.size - 2]

    val isPolygonClosed = geometry.isClosed()

    if (isPolygonClosed) {
      removeTooltipMarker()
      return
    }

    val midPoint =
      getMidPoint(
        LatLng(secondLastVertex.lat, secondLastVertex.lng),
        LatLng(lastVertex.lat, lastVertex.lng),
      )

    val distanceText = geometry.tooltipText ?: return

    if (tooltipMarker == null) {
      tooltipMarker = addTooltipMarker(map, midPoint, distanceText)
    } else {
      tooltipMarker?.position = midPoint
      tooltipMarker?.setIcon(createTextMarker(distanceText))
    }
  }

  private fun removeTooltipMarker() {
    tooltipMarker?.remove()
    tooltipMarker = null
  }

  private fun getMidPoint(p1: LatLng, p2: LatLng): LatLng {
    val lat = (p1.latitude + p2.latitude) / 2
    val lng = (p1.longitude + p2.longitude) / 2
    return LatLng(lat, lng)
  }

  private fun addTooltipMarker(map: GoogleMap, position: LatLng, text: String): Marker {
    val markerOptions =
      MarkerOptions().position(position).icon(createTextMarker(text)).anchor(0.5f, 1f)

    return map.addMarker(markerOptions)!!
  }

  private fun createTextMarker(text: String): BitmapDescriptor {
    val paint =
      Paint().apply {
        color = Color.BLACK
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }

    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val bitmap =
      Bitmap.createBitmap(bounds.width() + 40, bounds.height() + 40, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint =
      Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
      }

    val rectF = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
    canvas.drawRoundRect(rectF, 20f, 20f, bgPaint)

    canvas.drawText(text, (bitmap.width / 2).toFloat(), (bitmap.height / 1.5).toFloat(), paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }
}
