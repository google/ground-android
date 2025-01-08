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

  /** Returns the resolved zoom level based on request parameters. */
  fun getZoomLevel(currentZoomLevel: Float) =
    if (isAllowZoomOut) zoomLevel else max(zoomLevel, currentZoomLevel)
}
