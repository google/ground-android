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
package com.google.android.ground.ui.map

import com.google.android.ground.model.geometry.Coordinates
import kotlin.math.max

data class CameraUpdateRequest(val newPosition: NewPosition, val shouldAnimate: Boolean = false)

sealed class NewPosition

data class NewPositionViaBounds(val bounds: Bounds, val padding: Int) : NewPosition()

data class NewPositionViaCoordinates(val coordinates: Coordinates) : NewPosition()

data class NewPositionViaCoordinatesAndZoomLevel(
  val coordinates: Coordinates,
  private val zoomLevel: Float,
  private val isAllowZoomOut: Boolean,
) : NewPosition() {

  /** Returns the resolved zoom level based on request parameters. */
  fun getZoomLevel(currentZoomLevel: Float) =
    if (isAllowZoomOut) zoomLevel else max(zoomLevel, currentZoomLevel)
}
