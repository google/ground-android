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

import java.lang.Double.doubleToLongBits
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ByteReaderTest {
  //  private lateinit var br: ByteReader
  //
  //  fun makeReader(byteArray: ByteArray) = ByteReader(ByteArrayInputStream(byteArray))
  //
  //  @Before
  //  fun setUp() {
  //    br = makeReader(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7))
  //    br.setByteOrder(ByteOrder.BIG_ENDIAN)
  //  }
  //
  //  @Test
  //  fun setOffset() {
  //    br.setOffset(4)
  //    assertEquals(4, br.getOffset())
  //  }
  //
  //  @Test
  //  fun readByte() {
  //    br.setOffset(2)
  //    assertEquals(2, br.readByte())
  //    assertEquals(3, br.readByte())
  //    assertEquals(4, br.readByte())
  //  }
  //
  //  @Test
  //  fun readInt() {
  //    assertEquals(0x00010203, br.readInt())
  //    assertEquals(0x04050607, br.readInt())
  //  }
  //
  //  @Test
  //  fun readUShort() {
  //    assertEquals(0x0001, br.readUnsignedShort())
  //    assertEquals(0x0203, br.readUnsignedShort())
  //  }
  //
  //  @Test
  //  fun readShort() {
  //    assertEquals(0x0001, br.readShort())
  //    assertEquals(0x0203, br.readShort())
  //  }
  //
  //  @Test
  //  fun readDouble() {
  //    br = makeReader(3.14.toByteArray() + 2.71.toByteArray())
  //    br.setByteOrder(ByteOrder.LITTLE_ENDIAN)
  //    assertEquals(3.14, br.readDouble())
  //    assertEquals(2.71, br.readDouble())
  //    assertEquals(16, br.getOffset())
  //  }
  //
  //  @Test
  //  fun readFloat() {
  //    br = makeReader(3.14f.toByteArray() + 2.71f.toByteArray())
  //    br.setByteOrder(ByteOrder.LITTLE_ENDIAN)
  //    assertEquals(3.14f, br.readFloat())
  //    assertEquals(2.71f, br.readFloat())
  //    assertEquals(8, br.getOffset())
  //  }
  //
  //  @Test
  //  fun readString() {
  //    br = makeReader("ABCD".toByteArray())
  //    br.setByteOrder(ByteOrder.BIG_ENDIAN)
  //    assertEquals("AB", br.readString(2))
  //    assertEquals("CD", br.readString(2))
  //    assertEquals(4, br.getOffset())
  //  }
  // }
  //
  // fun Float.toByteArray(): ByteArray {
  //  val byteArray = ByteArray(4)
  //  var bits = floatToIntBits(this)
  //  for (i in 0..3) {
  //    byteArray[i] = Integer.valueOf(bits).toByte()
  //    bits = bits shr 8
  //  }
  //  return byteArray
}

fun Double.toByteArray(): ByteArray {
  val byteArray = ByteArray(8)
  var bits = doubleToLongBits(this)
  for (i in 0..7) {
    byteArray[i] = bits.toByte()
    bits = bits shr 8
  }
  return byteArray
}
