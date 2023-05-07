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

import com.google.android.gms.maps.model.LatLngBounds
import java.io.File

/**
 * Downloads tiles across regions at multiple zoom levels.
 *
 * @param mogCollection the collection from which tiles will be fetched.
 * @param outputBasePath the base path on the local file system where tiles should be written.
 */
class MogTileDownloader(
  private val mogCollection: MogCollection,
  private val outputBasePath: String
) {
  /**
   * Downloads all tiles overlapping the Tiles are written to the object's [outputBasePath] in files
   * with paths of the form `{z}/{x}/{y}.jpg`.
   *
   * @param bounds the bounds used to constrain which tiles are retrieved. Only tiles within or
   *   overlapping these bounds are retrieved.
   * @param zoomRange the min. and max. zoom levels for which tiles should be retrieved. Defaults to
   *   all available tiles in the collection as determined by the
   *   [MogCollection.hiResMogMaxZoom].
   */
  suspend fun downloadTiles(
    bounds: LatLngBounds,
    zoomRange: IntRange = 0..mogCollection.hiResMogMaxZoom
  ) =
    mogCollection.getTiles(bounds, zoomRange).collect { (coordinates, tile) ->
      val (x, y, zoom) = coordinates
      val path = File(outputBasePath, "$zoom/$x")
      path.mkdirs()
      File(path, "$y.jpg").writeBytes(tile.data!!)
    }
}
