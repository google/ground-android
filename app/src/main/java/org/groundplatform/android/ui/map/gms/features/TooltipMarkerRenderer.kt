/*
 * Copyright 2025 Google LLC
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import javax.inject.Inject

class TooltipMarkerRenderer @Inject constructor() {
  private var tooltipMarker: Marker? = null

  fun update(map: GoogleMap, position: LatLng, tooltipText: String?) {
    if (tooltipText.isNullOrBlank()) {
      remove()
      return
    }

    if (tooltipMarker == null) {
      tooltipMarker = create(map, position, tooltipText)
    } else {
      tooltipMarker?.position = position
      tooltipMarker?.setIcon(createTextMarker(tooltipText))
    }
  }

  fun remove() {
    tooltipMarker?.remove()
    tooltipMarker = null
  }

  private fun create(map: GoogleMap, position: LatLng, text: String): Marker {
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
