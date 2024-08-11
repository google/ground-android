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

data class CameraUpdateRequest(val newPosition: NewPosition, val shouldAnimate: Boolean = false)

sealed class NewPosition

// Note: Prefer setting the position via coordinates as it doesn't cause the map to zoom out even if
// nothing has changed. This is probably because the bound calculation is approximate, causing the
// zoom levels to change slightly each time. Currently, we only set the position via bounds when
// there are no saved positions i.e. on first open.
data class NewPositionViaBounds(val bounds: Bounds) : NewPosition()

data class NewPositionViaCoordinates(
  val coordinates: Coordinates,
  val zoomLevel: Float? = null,
  val isAllowZoomOut: Boolean = false,
) : NewPosition()
