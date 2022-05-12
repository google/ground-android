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
package com.google.android.gnd.ui

import android.content.Context
import javax.inject.Singleton
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.gnd.R
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel
import android.graphics.PorterDuff
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

@Singleton
class MarkerIconFactory @Inject constructor(@ApplicationContext private val context: Context) {
    fun getMarkerBitmap(color: Int, currentZoomLevel: Float): Bitmap {
        val outline = AppCompatResources.getDrawable(context, R.drawable.ic_marker_outline)
        val fill = AppCompatResources.getDrawable(context, R.drawable.ic_marker_fill)
        val overlay = AppCompatResources.getDrawable(context, R.drawable.ic_marker_overlay)
        // TODO: Adjust size based on selection state.
        var scale = ResourcesCompat.getFloat(context.resources, R.dimen.marker_bitmap_default_scale)
        if (currentZoomLevel >= MapContainerViewModel.ZOOM_LEVEL_THRESHOLD) {
            scale = ResourcesCompat.getFloat(context.resources, R.dimen.marker_bitmap_zoomed_scale)
        }
        val width = (outline!!.intrinsicWidth * scale).toInt()
        val height = (outline.intrinsicHeight * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        outline.setBounds(0, 0, width, height)
        outline.draw(canvas)
        fill!!.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        fill.setBounds(0, 0, width, height)
        fill.draw(canvas)
        overlay!!.setBounds(0, 0, width, height)
        overlay.draw(canvas)
        return bitmap
    }

    fun getMarkerIcon(@ColorInt color: Int, currentZoomLevel: Float): BitmapDescriptor {
        val bitmap = getMarkerBitmap(color, currentZoomLevel)
        // TODO: Cache rendered bitmaps.
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
