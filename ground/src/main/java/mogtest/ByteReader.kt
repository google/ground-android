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
package mogtest

import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.*

@Deprecated(" delete me")
class ByteReader(private val bytes: ByteArray) {
  var offset = 0
  var byteOrder: ByteOrder = ByteOrder.nativeOrder()

  fun setOffset(newOffset: Long) {
    //    println("STEP: ${this.nextByte} -> $nextByte")
//    require(newPos >= this.pos) { "Can't scan backwards in stream" }
    if (newOffset >= bytes.size) {
      error("Byte offset out of range. Total Bytes: " + bytes.size + ", Byte offset: " + newOffset)
    }
    this.offset = newOffset.toInt()
  }

  /**
   * Read a String from the provided number of bytes
   *
   * @param num number of bytes
   * @return String
   * @throws UnsupportedEncodingException upon string encoding error
   */
  @Throws(UnsupportedEncodingException::class)
  fun readString(num: Int): String? {
    val value = readString(offset, num)
    offset += num
    return value
  }

  /**
   * Read a String from the provided number of bytes
   *
   * @param offset byte offset
   * @param num number of bytes
   * @return String
   * @throws UnsupportedEncodingException upon string encoding error
   */
  @Throws(UnsupportedEncodingException::class)
  fun readString(offset: Int, num: Int): String? {
    verifyRemainingBytes(offset, num)
    var value: String? = null
    if (num != 1 || bytes[offset].toInt() != 0) {
      value = String(bytes, offset, num, StandardCharsets.US_ASCII)
    }
    return value
  }

  /**
   * Read a byte
   *
   * @return byte
   */
  fun readByte(): Byte {
    val value = readByte(offset)
    offset++
    return value
  }

  /**
   * Read a byte
   *
   * @param offset byte offset
   * @return byte
   */
  fun readByte(offset: Int): Byte {
    verifyRemainingBytes(offset, 1)
    return bytes[offset]
  }

  /**
   * Read an unsigned byte
   *
   * @return unsigned byte as short
   */
  fun readUnsignedByte(): Short {
    val value = readUnsignedByte(offset)
    offset++
    return value
  }

  /**
   * Read an unsigned byte
   *
   * @param offset byte offset
   * @return unsigned byte as short
   */
  fun readUnsignedByte(offset: Int): Short {
    return (readByte(offset).toInt() and 0xff).toShort()
  }

  /**
   * Read a short
   *
   * @return short
   */
  fun readShort(): Short {
    val value = readShort(offset)
    offset += 2
    return value
  }

  /**
   * Read a short
   *
   * @param offset byte offset
   * @return short
   */
  fun readShort(offset: Int): Short {
    verifyRemainingBytes(offset, 2)
    return ByteBuffer.wrap(bytes, offset, 2).order(byteOrder).short
  }

  /**
   * Read an unsigned short
   *
   * @return unsigned short as int
   */
  fun readUnsignedShort(): Int {
    val value = readUnsignedShort(offset)
    offset += 2
    return value
  }

  /**
   * Read an unsigned short
   *
   * @param offset byte offset
   * @return unsigned short as int
   */
  fun readUnsignedShort(offset: Int): Int {
    return readShort(offset).toInt() and 0xffff
  }

  /**
   * Read an integer
   *
   * @return integer
   */
  fun readInt(): Int {
    val value = readInt(offset)
    offset += 4
    return value
  }

  /**
   * Read an integer
   *
   * @param offset byte offset
   * @return integer
   */
  fun readInt(offset: Int): Int {
    verifyRemainingBytes(offset, 4)
    return ByteBuffer.wrap(bytes, offset, 4).order(byteOrder).int
  }

  /**
   * Read an unsigned int
   *
   * @return unsigned int as long
   */
  fun readUnsignedInt(): Long {
    val value = readUnsignedInt(offset)
    offset += 4
    return value
  }

  /**
   * Read an unsigned int
   *
   * @param offset byte offset
   * @return unsigned int as long
   */
  fun readUnsignedInt(offset: Int): Long {
    return readInt(offset).toLong() and 0xffffffffL
  }

  /**
   * Read a float
   *
   * @return float
   */
  fun readFloat(): Float {
    val value = readFloat(offset)
    offset += 4
    return value
  }

  /**
   * Read a float
   *
   * @param offset byte offset
   * @return float
   */
  fun readFloat(offset: Int): Float {
    verifyRemainingBytes(offset, 4)
    return ByteBuffer.wrap(bytes, offset, 4).order(byteOrder).float
  }

  /**
   * Read a double
   *
   * @return double
   */
  fun readDouble(): Double {
    val value = readDouble(offset)
    offset += 8
    return value
  }

  /**
   * Read a double
   *
   * @param offset byte offset
   * @return double
   */
  fun readDouble(offset: Int): Double {
    verifyRemainingBytes(offset, 8)
    return ByteBuffer.wrap(bytes, offset, 8).order(byteOrder).double
  }

  /**
   * Verify with the remaining bytes that there are enough remaining to read the provided amount
   *
   * @param offset byte offset
   * @param bytesToRead number of bytes to read
   */
  private fun verifyRemainingBytes(offset: Int, bytesToRead: Int) {
    if (offset + bytesToRead > bytes.size) {
      error(
        "No more remaining bytes to read. Total Bytes: " +
          bytes.size +
          ", Byte offset: " +
          offset +
          ", Attempted to read: " +
          bytesToRead
      )
    }
  }
}
