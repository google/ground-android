/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.model.imagery

import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.mog.TileCoordinates

/** An area is a contiguous set of tiles that task a geodesic rectangle. */
data class OfflineArea(
  val id: String,
  val state: State,
  val bounds: Bounds,
  val name: String,
  /** The range of zoom levels downloaded. */
  val zoomRange: IntRange,
) {
  val tiles
    get() = zoomRange.flatMap { TileCoordinates.withinBounds(bounds, it) }

  enum class State {
    PENDING,
    IN_PROGRESS,
    DOWNLOADED,
    FAILED,
  }
}
