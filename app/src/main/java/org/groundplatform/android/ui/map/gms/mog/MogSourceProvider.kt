/*
 * Copyright 2025 Google LLC
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

/**
 * Provides [MogSource]s for MOG (Map Overlay GeoTIFF) collections.
 *
 * This class defines the remote file structure for MOG collections, including the default zoom
 * level range and the file path templates for tiles at different zoom levels.
 *
 * The file structure is assumed to be in-sync with the remote Firebase Storage.
 */
object MogSourceProvider {

  private const val DEFAULT_MOG_MIN_ZOOM = 8
  const val DEFAULT_MOG_MAX_ZOOM = 14

  // TODO: Make sub-paths configurable and stop hardcoding here.
  // Issue URL: https://github.com/google/ground-android/issues/1730
  private const val DEFAULT_MOG_TILE_DIR = "/offline-imagery/default/$DEFAULT_MOG_MIN_ZOOM"

  private val zoomLevelToFilePathTemplate =
    mapOf(
      0..<DEFAULT_MOG_MIN_ZOOM to "overview.tif",
      DEFAULT_MOG_MIN_ZOOM..DEFAULT_MOG_MAX_ZOOM to "{x}/{y}.tif",
    )

  val defaultMogSources: List<MogSource> =
    zoomLevelToFilePathTemplate.map { (zoomRange, filePath) ->
      MogSource(zoomRange, "$DEFAULT_MOG_TILE_DIR/$filePath")
    }
}
