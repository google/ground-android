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

/**
 * A single cloud-optimized GeoTIFF file. Only headers and derived metadata are stored in memory;
 * image data is loaded lazily on demand.
 */
class Cog(val extent: TileCoordinates, imageHeaders: List<CogImage>) {
  val imagesByZoomLevel = imageHeaders.associateBy { it.zoomLevel }

  override fun toString(): String {
    return "Cog(extent=$extent, imagesByZoomLevel=$imagesByZoomLevel)"
  }
}
