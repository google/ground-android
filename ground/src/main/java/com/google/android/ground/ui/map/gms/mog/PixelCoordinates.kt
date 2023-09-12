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
package com.google.android.ground.ui.map.gms.mog

import com.google.android.gms.maps.model.LatLng
import kotlin.math.ln
import kotlin.math.tan

data class PixelCoordinates(val x: Int, val y: Int, val zoom: Int) {
  fun atZoom(targetZoom: Int): PixelCoordinates {
    val delta = targetZoom - zoom
    return PixelCoordinates(x.shiftLeft(delta), y.shiftLeft(delta), targetZoom)
  }
}

fun LatLng.toPixelCoordinates(zoom: Int): PixelCoordinates {
  val zoomFactor = 1 shl zoom
  val latRad = this.latitude.toRadians()
  val x = zoomFactor * (this.longitude + 180) / 360
  val y = zoomFactor * (1 - (ln(tan(latRad) + sec(latRad)) / Math.PI)) / 2
  return PixelCoordinates((x * 256.0).toInt(), (y * 256.0).toInt(), zoom)
}

fun TileCoordinates.toPixelCoordinate(xOffset: Int, yOffset: Int) =
  PixelCoordinates(x * 256 + xOffset, y * 256 + yOffset, zoom)
