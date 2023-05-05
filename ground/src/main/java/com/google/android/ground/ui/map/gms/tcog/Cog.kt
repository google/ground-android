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

package com.google.android.ground.ui.map.gms.tcog

import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Contiguous tiles are fetched in a single request. To minimize the number of server requests, we
 * also allow additional unneeded tiles to be fetched so that nearly contiguous tiles can be also
 * fetched in a single request. This constant defines the maximum number of unneeded bytes which can
 * be fetched per tile to allow nearly contiguous regions to be merged. Tiles are typically 10-20
 * KB, so allowing 20 KB over fetch generally allows 1-2 extra tiles to be fetched.
 */
const val MAX_OVER_FETCH_PER_TILE = 1 * 20 * 1024

data class RequestRange(
  var byteRange: LongRange,
  var tileCoordinates: MutableList<TileCoordinates>
)

/**
 * A single cloud-optimized GeoTIFF file. Only headers and derived metadata are stored in memory;
 * image data is loaded lazily on demand.
 */
class Cog(val url: String, val extent: TileCoordinates, imageHeaders: List<CogImage>) {
  val imagesByZoom = imageHeaders.associateBy { it.zoom }

  override fun toString(): String {
    return "Cog(extent=$extent, imagesByZoom=$imagesByZoom)"
  }

  private fun parseTiles(
    tileCoordinates: List<TileCoordinates>,
    inputStream: InputStream
  ): Flow<CogTile> = flow {
    var pos: Long? = null
    for (coords in tileCoordinates) {
      val image = imagesByZoom[coords.zoom]!!
      val byteRange = image.getByteRange(coords.x, coords.y) ?: error("$coords out of image bounds")
      if (pos != null && pos < byteRange.first) {
        while (pos++ < byteRange.first) {
          if (inputStream.read() == -1) {
            throw CogException("Unexpected end of tile response")
          }
        }
      }
      val startTimeMillis = System.currentTimeMillis()
      // TODO: Only create parser once per image or pass image into parser.
      val imageBytes = CogTileParser(image).parseTile(inputStream, byteRange.count())
      val time = System.currentTimeMillis() - startTimeMillis
      Timber.d("Fetched tile ${coords}: ${imageBytes.size} in $time ms")
      emit(CogTile(coords, image.tileWidth, image.tileLength, imageBytes))
      pos = byteRange.last + 1
    }
  }

  // TODO: Pass cogSource to constructor instead of here.
  fun getTiles(cogSource: CogSource, tileCoordinatesList: List<TileCoordinates>): Flow<CogTile> =
    flow {
      val requestRanges = mutableListOf<RequestRange>()
      for (tileCoords in tileCoordinatesList) {
        val byteRange = getByteRange(tileCoords) ?: continue
        val prev = if (requestRanges.isEmpty()) null else requestRanges.last()
        if (prev == null || byteRange.first - prev.byteRange.last - 1 > MAX_OVER_FETCH_PER_TILE) {
          requestRanges.add(RequestRange(byteRange, mutableListOf(tileCoords)))
        } else {
          prev.byteRange = LongRange(prev.byteRange.first, byteRange.last)
          prev.tileCoordinates.add(tileCoords)
        }
      }
      // TODO: Use thread pool to request multiple ranges in parallel.
      requestRanges.forEach { (byteRange, tileCoords) ->
        Timber.d("Fetching $byteRange")
        cogSource.openStream(url, byteRange)?.use { emitAll(parseTiles(tileCoords, it)) }
      }
    }

  private fun getByteRange(tileCoordinate: TileCoordinates): LongRange? =
    imagesByZoom[tileCoordinate.zoom]?.getByteRange(tileCoordinate.x, tileCoordinate.y)

  suspend fun getTile(cogSource: CogSource, tileCoordinates: TileCoordinates): CogTile =
    getTiles(cogSource, listOf(tileCoordinates)).first()
}
