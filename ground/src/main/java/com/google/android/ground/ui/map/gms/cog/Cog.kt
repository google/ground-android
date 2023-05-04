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
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import timber.log.Timber

data class RequestRange(
  var byteRange: LongRange,
  var tileCoordinates: MutableList<TileCoordinates>
)

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
    tileCoordinates: List<TileCoordinates>,
    inputStream: InputStream
  ): Flow<Result<CogTile>> = flow {
    var pos: Long? = null
    for (coords in tileCoordinates) {
      val image = imagesByZoomLevel[coords.zoomLevel]!!
      // TODO: Only create parser once per image.
      // TODO: Support non-contiguous tile byte ranges.
      val byteRange = image.getByteRange(coords.x, coords.y)!!
      if (pos != null && pos < byteRange.first) {
        while (pos++ < byteRange.first) {
          if (inputStream.read() == -1) {
            throw CogException("Unexpected end of tile response")
          }
        }
      }
      emit(success(CogTileParser(image).parseTile(coords, inputStream)))
      pos = byteRange.last + 1
    }
  }

  fun getTiles(cogSource: CogSource, bounds: LatLngBounds, zoomLevels: IntRange) =
    getTiles(cogSource, zoomLevels.flatMap { TileCoordinates.withinBounds(bounds, it) })

  // TODO: Pass cogSource to constructor instead of here.
  private fun getTiles(
    cogSource: CogSource,
    tileCoordinates: List<TileCoordinates>
  ): Flow<Result<CogTile>> = flow {
    try {
      val requestRanges = mutableListOf<RequestRange>()
      // TODO: Support non contiguous ranges.
      // Tiles are typically 10-20 KB. Allow extra 5 K to allow us to combine ranges into a
      // single request.
      val maxOverfetch = 5 * 1024
      for (coords in tileCoordinates) {
        val byteRange = getByteRange(coords) ?: continue
        val prev = if (requestRanges.isEmpty()) null else requestRanges.last()
        if (prev == null || byteRange.first - prev.byteRange.last - 1 > maxOverfetch) {
          requestRanges.add(RequestRange(byteRange, mutableListOf(coords)))
        } else {
          prev.byteRange = LongRange(prev.byteRange.first, byteRange.last)
          prev.tileCoordinates.add(coords)
        }
      }
      // We need:
      // List<LongRange> ranges to request
      // For each request range: List<Pair<Int, TileCoordinate?> - # bytes + tile no.
      // List<Pair<LongRange, List<TileCoordinate | SkipBytes>T>
      // TODO: Use thread pool to request multiple ranges in parallel.
      requestRanges.forEach { (byteRange, coords) ->
        Timber.d("Fetching $byteRange")
        cogSource.openStream(url, byteRange)?.use { emitAll(parseTiles(coords, it)) }
      }
    } catch (e: Exception) {
      emit(failure(e))
    }
  }

  private fun getByteRange(tileCoordinate: TileCoordinates): LongRange? =
    imagesByZoomLevel[tileCoordinate.zoomLevel]?.getByteRange(tileCoordinate.x, tileCoordinate.y)

  fun getTile(cogSource: CogSource, tileCoordinate: TileCoordinates): CogTile = runBlocking {
    getTiles(cogSource, listOf(tileCoordinate)).first().getOrThrow()
  }
}
