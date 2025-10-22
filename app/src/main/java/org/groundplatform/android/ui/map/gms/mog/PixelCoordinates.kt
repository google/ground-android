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

import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

/** Represents pixel coordinates of a point on a map at a specific zoom level. */
data class PixelCoordinates(val x: Int, val y: Int, val zoom: Int) {

  /** Converts these pixel coordinates to another zoom level. */
  fun atZoom(targetZoom: Int): PixelCoordinates {
    val zoomDelta = targetZoom - zoom
    return PixelCoordinates(x = x shl zoomDelta, y = y shl zoomDelta, zoom = targetZoom)
  }
}

/**
 * Converts a [LatLng] coordinate to its corresponding [PixelCoordinates] at a given zoom level.
 *
 * Uses the Web Mercator projection formula:
 * - X maps longitude linearly
 * - Y maps latitude via Mercator transformation
 */
fun LatLng.toPixelCoordinates(zoom: Int): PixelCoordinates {
  val zoomFactor = 1 shl zoom
  val latRadians = latitude.toRadians()
  val normalizedX = (longitude + 180.0) / 360.0
  val normalizedY = (1.0 - ln(tan(latRadians) + sec(latRadians)) / Math.PI) / 2.0

  val pixelX = (normalizedX * zoomFactor * 256.0).toInt()
  val pixelY = (normalizedY * zoomFactor * 256.0).toInt()

  return PixelCoordinates(pixelX, pixelY, zoom)
}

/** Converts [TileCoordinates] to [PixelCoordinates] by applying pixel offsets within the tile. */
fun TileCoordinates.toPixelCoordinate(xOffset: Int, yOffset: Int) =
  PixelCoordinates(x = x * 256 + xOffset, y = y * 256 + yOffset, zoom = zoom)

/**
 * Converts a pixel position within a tile back to a geographic [LatLng]. This performs the inverse
 * Web Mercator projection.
 */
fun TileCoordinates.pixelToLatLng(pixelX: Int, pixelY: Int): LatLng {
  val zoomFactor = 1 shl zoom
  val normalizedX = (x * 256.0 + pixelX) / (zoomFactor * 256.0)
  val normalizedY = (y * 256.0 + pixelY) / (zoomFactor * 256.0)

  val longitude = normalizedX * 360.0 - 180.0
  val latitudeRadians = atan(sinh(Math.PI * (1.0 - 2.0 * normalizedY)))
  val latitude = Math.toDegrees(latitudeRadians)

  return LatLng(latitude, longitude)
}
