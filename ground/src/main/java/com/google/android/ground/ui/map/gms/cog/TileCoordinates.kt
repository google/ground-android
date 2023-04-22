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

package com.google.android.ground.ui.map.gms.cog

import kotlin.math.abs

data class TileCoordinates(val x: Int, val y: Int, val zoom: Int) {
  fun originAtZoom(targetZoom: Int): TileCoordinates {
    val zoomDelta = targetZoom - zoom
    return if (zoomDelta > 0) {
      TileCoordinates(x shl zoomDelta, y shl zoomDelta, targetZoom)
    } else {
      TileCoordinates(x shr abs(zoomDelta), y shr abs(zoomDelta), targetZoom)
    }
  }
}
