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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import javax.inject.Inject

/**
 * Responsible for rendering a tooltip marker on a GoogleMap instance.
 *
 * This class handles creating, updating, and removing a marker that displays custom tooltip text.
 * It uses a bitmap with rounded corners and stylized text to show the tooltip content.
 */
class TooltipMarkerRenderer @Inject constructor() {
  private var tooltipMarker: Marker? = null

  /**
   * Shows or updates the tooltip marker on the map.
   *
   * If the marker doesn't exist, a new one is created. If it does, its position and text are
   * updated.
   *
   * @param map The [GoogleMap] instance to render on.
   * @param midPoint The [LatLng] position for the tooltip.
   * @param tooltipText The text to display. A null or blank string with hide the marker.
   */
  fun show(map: GoogleMap, midPoint: LatLng?, tooltipText: String?) {
    if (midPoint == null || tooltipText.isNullOrBlank()) {
      remove()
      return
    }

    val currentMarker = tooltipMarker
    if (currentMarker == null) {
      tooltipMarker =
        map.addMarker(
          MarkerOptions()
            .position(midPoint)
            .icon(createTextMarker(tooltipText))
            .anchor(ANCHOR_U, ANCHOR_V)
        )
    } else {
      currentMarker.position = midPoint
      currentMarker.setIcon(createTextMarker(tooltipText))
    }
  }

  /** Removes the current tooltip marker from the map. */
  private fun remove() {
    tooltipMarker?.remove()
    tooltipMarker = null
  }

  private fun createTextMarker(text: String): BitmapDescriptor {
    val textPaint =
      Paint().apply {
        color = Color.BLACK
        textSize = TEXT_SIZE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }

    val textBounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)

    val bitmapWidth = textBounds.width() + H_PADDING
    val bitmapHeight = textBounds.height() + V_PADDING

    val bitmap = createBitmap(bitmapWidth, bitmapHeight)
    val canvas = Canvas(bitmap)

    val bgPaint =
      Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
      }

    val rectF = RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())
    canvas.drawRoundRect(rectF, CORNER_RADIUS, CORNER_RADIUS, bgPaint)

    // Calculate y-coordinate for vertically centered text.
    val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)

    canvas.drawText(text, bitmap.width / 2f, yPos, textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  companion object {
    private const val TEXT_SIZE = 40f
    private const val H_PADDING = 40
    private const val V_PADDING = 30
    private const val CORNER_RADIUS = 20f
    private const val ANCHOR_U = 0.5f
    private const val ANCHOR_V = 1.0f
  }
}
