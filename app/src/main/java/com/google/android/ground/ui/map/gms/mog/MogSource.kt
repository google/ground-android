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

/**
 * Provides URLs or relative paths of one or more MOGs partitioned by web mercator tile extents at a
 * particular range of zoom level. Relative paths are assumed to be managed by RemoteStorageManager.
 *
 * Examples:
 * ```
 *   // Tiles for zoom levels 0-7 (inclusive) contained in a single file:
 *   val world = MogSource(0..7, "https://storage.googleapis.com/my-bucket/world.tif")
 *   val world = MogSource(0..7, "/offline-imagery/default/{z}/overview.tif")
 *
 *   // Tiles for zoom levels 8-14 (inclusive) contained in separate files, partitioned by zoom level
 *   // 8 tile extents:
 *   val region = MogSource(8..14, "https://storage.googleapis.com/my-bucket/{x}/{y}.tif")
 *   val region = MogSource(8..14, "/offline-imagery/default/{z}/{x}/{y}.tif")
 * ```
 */
data class MogSource(val zoomRange: IntRange, val pathTemplate: String) {
  /** Returns the bounds of the MOG containing the tile with the specified coordinates. */
  fun getMogBoundsForTile(tileCoordinates: TileCoordinates): TileCoordinates {
    check(zoomRange.contains(tileCoordinates.zoom)) {
      "Tile coordinates zoom ${tileCoordinates.zoom} must be within source zoom range $zoomRange"
    }
    return tileCoordinates.originAtZoom(zoomRange.first)
  }

  fun getMogPath(mogBounds: TileCoordinates): String {
    check(zoomRange.first == mogBounds.zoom) {
      "Bounds zoom ${mogBounds.zoom} must match source min zoom ${zoomRange.first}"
    }
    return pathTemplate
      .replace("{z}", mogBounds.zoom.toString())
      .replace("{x}", mogBounds.x.toString())
      .replace("{y}", mogBounds.y.toString())
  }
}

fun List<MogSource>.minZoom() = minOf { it.zoomRange.first }

fun List<MogSource>.maxZoom() = maxOf { it.zoomRange.last }

fun List<MogSource>.zoomRange() = IntRange(minZoom(), maxZoom())
