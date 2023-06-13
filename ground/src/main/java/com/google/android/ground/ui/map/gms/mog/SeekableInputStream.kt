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

class SeekableInputStream(private val sourceStream: InputStream) : InputStream() {
  private var pos = 0
  private val buffer = ByteBuffer.allocate(256 * 1024)
  private var mark = 0

  val position: Int
    get() = pos

  override fun read(): Int {
    val i: Int
    if (pos < buffer.position()) {
      i = buffer.get(pos).toUByte().toInt()
    } else {
      i = sourceStream.read()
      if (i < 0) return i
      buffer.put(i.toByte())
    }
    pos++
    return i
  }

  fun seek(newPos: Int) {
    if (newPos < pos) {
      this.pos = newPos
    } else {
      repeat(newPos - pos) {
        val n = read()
        if (n == -1) error("Unexpected EOF")
      }
    }
  }

  override fun mark(readlimit: Int) {
    this.mark = pos
  }

  fun mark() {
    mark(Int.MAX_VALUE)
  }

  override fun reset() {
    seek(this.mark)
  }
}
