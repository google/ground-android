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

import com.google.android.gms.maps.model.Tile
import java.io.InputStream
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Contiguous tiles are fetched in a single request. To minimize the number of server requests, we
 * also allow additional unneeded tiles to be fetched so that nearly contiguous tiles can be also
 * fetched in a single request. This constant defines the maximum number of unneeded bytes which can
 * be fetched per tile to allow nearly contiguous regions to be merged. Tiles are typically 10-20
 * KB, so allowing 20 KB over fetch generally allows 1-2 extra tiles to be fetched.
 */
const val MAX_OVER_FETCH_PER_TILE = 1 * 20 * 1024

/**
 * A single Maps Optimized GeoTIFF (MOG). MOGs are [Cloud Optimized GeoTIFFs (COGs)](cogeo.org)
 * clipped and configured for visualization with Google Maps Platform. This class stores metadata
 * and fetches tiles on demand via [getTile] and [getTiles].
 */
class Mog(val url: String, val extent: TileCoordinates, images: List<MogImage>) {
  private val imagesByZoom = images.associateBy { it.zoom }

  suspend fun getTile(tileCoordinates: TileCoordinates): Tile? =
    fetchTiles(getTilesRequests(listOf(tileCoordinates)).first()).firstOrNull()?.second

  fun getTilesRequests(tileCoordinatesList: List<TileCoordinates>): List<TilesRequest> {
    val tilesRequests = mutableListOf<MutableTilesRequest>()
    for (tileCoordinates in tileCoordinatesList) {
      val byteRange = getByteRange(tileCoordinates) ?: continue
      val prev = tilesRequests.lastOrNull()
      if (prev == null || byteRange.first - prev.byteRange.last - 1 > MAX_OVER_FETCH_PER_TILE) {
        tilesRequests.add(
          MutableTilesRequest(url, extent, byteRange, mutableListOf(tileCoordinates))
        )
      } else {
        prev.extentRange(byteRange.last, tileCoordinates)
      }
    }
    return tilesRequests.map { it.toTilesRequest() }
  }

  // TODO: Use thread pool to request multiple ranges in parallel.
  fun fetchTiles(tilesRequest: TilesRequest): Flow<Pair<TileCoordinates, Tile>> = flow {
    UrlInputStream(tilesRequest.imageUrl, tilesRequest.byteRange).use {
      emitAll(parseTiles(tilesRequest.tileCoordinatesList, it))
    }
  }

  override fun toString(): String {
    return "Mog(url=$url, imagesByZoom=$imagesByZoom)"
  }

  private fun parseTiles(
    tileCoordinatesList: List<TileCoordinates>,
    inputStream: InputStream
  ): Flow<Pair<TileCoordinates, Tile>> = flow {
    var pos: Long? = null
    for (tileCoordinates in tileCoordinatesList) {
      val image = imagesByZoom[tileCoordinates.zoom]!!
      val byteRange =
        image.getByteRange(tileCoordinates.x, tileCoordinates.y)
          ?: error("$tileCoordinates out of image bounds")
      if (pos != null && pos < byteRange.first) {
        while (pos++ < byteRange.first) {
          if (inputStream.read() == -1) {
            error("Unexpected end of tile response")
          }
        }
      }
      val startTimeMillis = System.currentTimeMillis()
      val imageBytes = image.parseTile(inputStream, byteRange.count())
      val time = System.currentTimeMillis() - startTimeMillis
      Timber.d("Fetched tile ${tileCoordinates}: ${imageBytes.size} in $time ms")
      emit(Pair(tileCoordinates, Tile(image.tileWidth, image.tileLength, imageBytes)))
      pos = byteRange.last + 1
    }
  }

  private fun getByteRange(tileCoordinate: TileCoordinates): LongRange? =
    imagesByZoom[tileCoordinate.zoom]?.getByteRange(tileCoordinate.x, tileCoordinate.y)
}
