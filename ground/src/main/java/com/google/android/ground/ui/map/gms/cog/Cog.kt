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

import com.google.android.gms.maps.model.LatLngBounds
import java.io.InputStream
import kotlin.Result.Companion.success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

/**
 * A single cloud-optimized GeoTIFF file. Only headers and derived metadata are stored in memory;
 * image data is loaded lazily on demand.
 */
class Cog(val url: String, val extent: TileCoordinates, imageHeaders: List<CogImage>) {
  val imagesByZoomLevel = imageHeaders.associateBy { it.zoomLevel }

  override fun toString(): String {
    return "Cog(extent=$extent, imagesByZoomLevel=$imagesByZoomLevel)"
  }

  fun parseTiles(
    tileCoordinates: MutableList<TileCoordinates>,
    inputStream: InputStream
  ): Flow<Result<CogTile>> = flow {
    for (tileCoordinate in tileCoordinates) {
      val image = imagesByZoomLevel[tileCoordinate.zoomLevel]!!
      emit(success(image.parseTile(tileCoordinate, inputStream)))
    }
  }

  fun getTiles(cogSource: CogSource, bounds: LatLngBounds, zoomLevels: IntRange) =
    getTiles(cogSource, zoomLevels.flatMap { TileCoordinates.withinBounds(bounds, it) })

  private fun getTiles(
    cogSource: CogSource,
    tileCoordinates: List<TileCoordinates>
  ): Flow<Result<CogTile>> = flow {
    val availableTileCoordinates = mutableListOf<TileCoordinates>()
    val byteRanges = mutableListOf<LongRange>()
    for (tileCoordinate in tileCoordinates) {
      val byteRange = getByteRange(tileCoordinate) ?: continue
      availableTileCoordinates.add(tileCoordinate)
      byteRanges.add(byteRange)
    }
    // TODO: Split large numbers of ranges into multiple requests.
    // TODO: Merge contiguous ranges when adding to header.
    //            if (image != null) emitAll(image.getTiles(bounds))
    cogSource.openStream(url, byteRanges)?.use { emitAll(parseTiles(availableTileCoordinates, it)) }
  }

  private fun getByteRange(tileCoordinate: TileCoordinates): LongRange? =
    imagesByZoomLevel[tileCoordinate.zoomLevel]?.getByteRange(tileCoordinate.x, tileCoordinate.y)

  fun getTile(cogSource: CogSource, tileCoordinate: TileCoordinates): CogTile = runBlocking {
    getTiles(cogSource, listOf(tileCoordinate)).first().getOrThrow()
  }
}
