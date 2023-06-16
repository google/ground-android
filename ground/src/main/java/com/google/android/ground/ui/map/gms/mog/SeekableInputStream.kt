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
import java.nio.ByteBuffer

/** Implementation of [InputStream] which allows reading bytes from a given position directly. */
class SeekableInputStream(private val inputStream: InputStream) : InputStream() {
  private var currentPosition = 0
  private val buffer = ByteBuffer.allocate(BUFFER_BYTES)
  private var lastMarkedPosition = 0

  override fun read(): Int {
    val data: Int
    if (currentPosition < buffer.position()) {
      data = buffer.get(currentPosition).toUByte().toInt()
    } else {
      data = inputStream.read()
      if (data == END_OF_STREAM) return data
      buffer.put(data.toByte())
    }
    currentPosition++
    return data
  }

  /** Moves the pointer to the new position. */
  fun seek(newPosition: Int) {
    if (newPosition < currentPosition) {
      currentPosition = newPosition
    } else {
      repeat(newPosition - currentPosition) {
        val data = read()
        if (data == END_OF_STREAM) error("Unexpected EOF")
      }
    }
  }

  override fun mark(readlimit: Int) {
    lastMarkedPosition = currentPosition
  }

  fun mark() {
    mark(Int.MAX_VALUE)
  }

  override fun reset() {
    seek(lastMarkedPosition)
  }

  companion object {
    private const val BUFFER_BYTES = 256 * 1024 // 256 KB
    private const val END_OF_STREAM = -1
  }
}
