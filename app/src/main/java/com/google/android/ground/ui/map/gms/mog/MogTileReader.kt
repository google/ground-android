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

import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

// TODO: Add unit tests.
// Issue URL: https://github.com/google/ground-android/issues/1596
/** Reads tiles from an [InputStream]. */
class MogTileReader(private val inputStream: InputStream, initialOffset: Long) {
  private var offset: Long = initialOffset

  fun readTiles(tiles: List<MogTileMetadata>): Flow<MogTile> = flow {
    // Tiles must be read sequentially since [inputStream] is re-used for each tile.
    tiles.forEach { emit(readTile(it)) }
  }

  /** Read and return the tile with the specified metadata. */
  private fun readTile(tileMetadata: MogTileMetadata): MogTile {
    val startTimeMillis = System.currentTimeMillis()
    val tileData = readTileData(tileMetadata.byteRange)
    val time = System.currentTimeMillis() - startTimeMillis
    Timber.d("Read tile ${tileMetadata.tileCoordinates}: ${tileData.size} in $time ms")
    return MogTile(tileMetadata, tileData)
  }

  private fun readTileData(byteRange: LongRange): ByteArray {
    // Skip bytes for non-contiguous tile byte ranges.
    skipToOffset(byteRange.first)

    return readTileData(byteRange.count())
  }

  private fun skipToOffset(newOffset: Long) {
    while (offset < newOffset) {
      if (inputStream.read() == -1) error("Unexpected end of tile response")
      offset++
    }
  }

  /** Reads and returns a tile. Doesn't close the stream. */
  private fun readTileData(numBytes: Int): ByteArray {
    val bytes = ByteArray(numBytes)
    var bytesRead = 0
    while (bytesRead < numBytes) {
      val b = inputStream.read()
      if (b < 0) break
      bytes[bytesRead++] = b.toByte()
      offset++
    }
    if (bytesRead < numBytes) {
      Timber.w("Too few bytes received. Expected $numBytes, got $bytesRead")
    }
    return bytes
  }
}
