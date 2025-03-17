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
package org.groundplatform.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.groundplatform.android.R
import org.groundplatform.android.ui.util.obtainTextPaintFromStyle

/** Responsible for building dynamically generated icon bitmaps. */
@Singleton
class IconFactory @Inject constructor(@ApplicationContext private val context: Context) {
  /** Create a scaled bitmap based on the dimensions of a given [Drawable]. */
  private fun createBitmap(drawable: Drawable, scale: Float = 1f): Bitmap {
    val width = (drawable.intrinsicWidth * scale).toInt()
    val height = (drawable.intrinsicHeight * scale).toInt()

    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  }

  /** Returns a [Bitmap] representing an individual marker pin on the map. */
  fun getMarkerBitmap(color: Int, scale: Float): Bitmap {
    val outline = AppCompatResources.getDrawable(context, R.drawable.ic_marker_outline)
    val fill = AppCompatResources.getDrawable(context, R.drawable.ic_marker_fill)
    val overlay = AppCompatResources.getDrawable(context, R.drawable.ic_marker_overlay)

    val bitmap = createBitmap(outline!!, scale)
    val canvas = Canvas(bitmap)

    outline.setBounds(0, 0, bitmap.width, bitmap.height)
    outline.draw(canvas)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      fill!!.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
    } else {
      fill!!.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }
    fill.setBounds(0, 0, bitmap.width, bitmap.height)
    fill.draw(canvas)
    overlay!!.setBounds(0, 0, bitmap.width, bitmap.height)
    overlay.draw(canvas)
    return bitmap
  }

  /** Returns a [BitmapDescriptor] for representing an individual marker on the map. */
  fun getMarkerIcon(color: Int, scale: Float): BitmapDescriptor {
    val bitmap = getMarkerBitmap(color, scale)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  /** Returns a [BitmapDescriptor] for representing a marker cluster on the map. */
  fun getClusterIcon(text: String): BitmapDescriptor {
    val fill = AppCompatResources.getDrawable(context, R.drawable.cluster_marker)
    val bitmap = createBitmap(fill!!)
    val canvas = Canvas(bitmap)

    val textPaint =
      context.applicationContext.obtainTextPaintFromStyle(
        R.style.TextAppearance_App_MapClusterLabel
      )
    val bounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, bounds)
    val x = (bitmap.width - bounds.width()) / 2
    val y = (bitmap.height + bounds.height()) / 2

    fill.setBounds(0, 0, bitmap.width, bitmap.height)
    fill.draw(canvas)

    canvas.drawText(text, x.toFloat(), y.toFloat(), textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }
}
