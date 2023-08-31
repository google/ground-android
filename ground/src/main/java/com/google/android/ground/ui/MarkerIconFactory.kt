/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.ground.Config
import com.google.android.ground.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkerIconFactory @Inject constructor(@ApplicationContext private val context: Context) {
  /** Create a scaled bitmap based on the dimensions of a given [Drawable]. */
  private fun createBitmap(
    drawable: Drawable,
    zoomLevel: Float,
    isSelected: Boolean = false,
  ): Bitmap {
    // TODO: Adjust size based on selection state.
    var scale = ResourcesCompat.getFloat(context.resources, R.dimen.marker_bitmap_default_scale)

    if (zoomLevel >= Config.ZOOM_LEVEL_THRESHOLD) {
      // Scale the drawable when we cross the app's zoom level threshold.
      scale = ResourcesCompat.getFloat(context.resources, R.dimen.marker_bitmap_zoomed_scale)
    }

    if (isSelected) {
      // TODO: Revisit scale for selected markers
      scale += 1
    }

    val width = (drawable.intrinsicWidth * scale).toInt()
    val height = (drawable.intrinsicHeight * scale).toInt()

    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  }

  /** Returns a [Bitmap] representing an individual marker on the map. */
  fun getMarkerBitmap(color: Int, currentZoomLevel: Float, isSelected: Boolean = false): Bitmap {
    val outline = AppCompatResources.getDrawable(context, R.drawable.ic_marker_outline)
    val fill = AppCompatResources.getDrawable(context, R.drawable.ic_marker_fill)
    val overlay = AppCompatResources.getDrawable(context, R.drawable.ic_marker_overlay)
    val bitmap = createBitmap(outline!!, currentZoomLevel, isSelected)
    val canvas = Canvas(bitmap)

    outline.setBounds(0, 0, bitmap.width, bitmap.height)
    outline.draw(canvas)
    fill!!.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    fill.setBounds(0, 0, bitmap.width, bitmap.height)
    fill.draw(canvas)
    overlay!!.setBounds(0, 0, bitmap.width, bitmap.height)
    overlay.draw(canvas)
    return bitmap
  }

  /** Returns a [BitmapDescriptor] for representing an individual marker on the map. */
  fun getMarkerIcon(
    @ColorInt color: Int,
    currentZoomLevel: Float,
    isSelected: Boolean = false
  ): BitmapDescriptor {
    val bitmap = getMarkerBitmap(color, currentZoomLevel, isSelected)
    // TODO: Cache rendered bitmaps.
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  /** Returns a [BitmapDescriptor] for representing a marker cluster on the map. */
  fun getClusterIcon(
    @ColorInt color: Int,
    currentZoomLevel: Float,
    text: String,
  ): BitmapDescriptor {
    val fill = AppCompatResources.getDrawable(context, R.drawable.cluster_marker)
    val bitmap = createBitmap(fill!!, currentZoomLevel, false)
    val canvas = Canvas(bitmap)
    val style = Paint()

    style.color = Color.BLACK
    style.textSize = 40.0.toFloat()

    val bounds = Rect()
    style.getTextBounds(text, 0, text.length, bounds)
    val x = (bitmap.width - bounds.width()) / 2
    val y = (bitmap.height + bounds.height()) / 2

    fill.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    fill.setBounds(0, 0, bitmap.width, bitmap.height)
    fill.draw(canvas)

    canvas.drawText(text, x.toFloat(), y.toFloat(), style)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }
}
