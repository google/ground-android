/*
 * Copyright 2023 Google LLC
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

package org.groundplatform.android.ui.map.gms.mog

import com.google.android.gms.maps.model.LatLngBounds

data class PixelBounds(val min: PixelCoordinates, val max: PixelCoordinates) {
  fun contains(pixelCoordinates: PixelCoordinates): Boolean {
    // Do pixel math at precision of point in highest resolution plane.
    val (minX, minY, _) = min.atZoom(pixelCoordinates.zoom)
    val (maxX, maxY, _) = max.atZoom(pixelCoordinates.zoom)
    val (x, y, _) = pixelCoordinates
    return x in minX..maxX && y in minY..maxY
  }
}

fun LatLngBounds.toPixelBounds(zoom: Int) =
  PixelBounds(northwest().toPixelCoordinates(zoom), southeast().toPixelCoordinates(zoom))
