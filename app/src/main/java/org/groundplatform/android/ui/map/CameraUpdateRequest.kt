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
package org.groundplatform.android.ui.map

import kotlin.math.max
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.map.Bounds

sealed class CameraUpdateRequest

data class NewCameraPositionViaBounds(
  val bounds: Bounds,
  val padding: Int,
  val shouldAnimate: Boolean = false,
) : CameraUpdateRequest()

data class NewCameraPositionViaCoordinates(
  val coordinates: Coordinates,
  val shouldAnimate: Boolean = false,
) : CameraUpdateRequest()

data class NewCameraPositionViaCoordinatesAndZoomLevel(
  val coordinates: Coordinates,
  private val zoomLevel: Float,
  private val isAllowZoomOut: Boolean,
  val shouldAnimate: Boolean = false,
) : CameraUpdateRequest() {

  /**
   * Determines the effective zoom level to use when centering or restoring the map view.
   *
   * Ensures we never zoom in or out beyond a safe range [2f, 21f]. When [isAllowZoomOut] is false,
   * prevents accidental zoom-out by keeping at least the current zoom level (avoids sudden
   * jump-outs when reloading or switching maps).
   */
  fun getZoomLevel(currentZoomLevel: Float): Float {
    val target = if (isAllowZoomOut) zoomLevel else max(zoomLevel, currentZoomLevel)
    return target.coerceIn(2f, 21f)
  }
}
