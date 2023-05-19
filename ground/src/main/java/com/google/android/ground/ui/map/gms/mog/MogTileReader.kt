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

class MogTileReader(private val inputStream: InputStream) {
  private var pos: Long = Long.MAX_VALUE

  fun readTiles(tiles: List<MogTileMetadata>): Flow<MogTile> = flow {
    tiles.forEach { emit(readTile(it)) }
  }

  private suspend fun readTile(metadata: MogTileMetadata): MogTile {
    val tileReader = MogTileReader(inputStream)
    val startTimeMillis = System.currentTimeMillis()
    val tileData = tileReader.readTileData(metadata.byteRange)
    val time = System.currentTimeMillis() - startTimeMillis
    Timber.d("Read tile ${metadata.tileCoordinates}: ${tileData.size} in $time ms")

    return MogTile(metadata, tileData)
  }

  private fun readTileData(byteRange: TileByteRange): ByteArray {
    // Skip bytes for non-contiguous tile byte ranges.
    skipToPos(byteRange.first)

    return readTileData(byteRange.count())
  }

  private fun skipToPos(newPos: Long) {
    if (newPos < pos) error("Can't scan backwards in input stream")
    while (newPos > pos) {
      if (inputStream.read() == -1) error("Unexpected end of tile response")
      pos++
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
      pos++
    }
    if (bytesRead < numBytes) {
      Timber.w("Too few bytes received. Expected $numBytes, got $bytesRead")
    }
    return bytes
  }
}
