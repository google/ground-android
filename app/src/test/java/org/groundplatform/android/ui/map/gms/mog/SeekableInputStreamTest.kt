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

package org.groundplatform.android.ui.map.gms.mog

import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test

class SeekableInputStreamTest {

  private val testSourceStream = ByteArrayInputStream(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
  private lateinit var inputStream: SeekableInputStream

  @Before
  fun setUp() {
    inputStream = SeekableInputStream(testSourceStream)
  }

  @Test
  fun `seek moves to correct position`() {
    assertEquals(inputStream.read(), 0)
    assertEquals(inputStream.read(), 1)
    inputStream.seek(8)
    assertEquals(inputStream.read(), 8)
    assertEquals(inputStream.read(), 9)
    inputStream.seek(1)
    assertEquals(inputStream.read(), 1)
    assertEquals(inputStream.read(), 2)
  }

  @Test
  fun `mark and reset`() {
    assertEquals(inputStream.read(), 0)
    assertEquals(inputStream.read(), 1)
    inputStream.mark()
    inputStream.seek(8)
    assertEquals(inputStream.read(), 8)
    assertEquals(inputStream.read(), 9)
    inputStream.reset()
    assertEquals(inputStream.read(), 2)
    assertEquals(inputStream.read(), 3)
  }
}
