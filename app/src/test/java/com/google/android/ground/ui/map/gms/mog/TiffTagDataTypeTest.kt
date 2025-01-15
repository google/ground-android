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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TiffTagDataTypeTest(
  private val dataType: TiffTagDataType,
  private val sizeInBytes: Int,
  private val position: Int,
) {

  @Test
  fun testByIndex() {
    assertThat(dataType.sizeInBytes).isEqualTo(sizeInBytes)
    assertThat(TiffTagDataType.byId(position)).isEqualTo(dataType)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0} should be at position {2} with size {1}")
    fun data() =
      listOf(
        arrayOf(TiffTagDataType.BYTE, 1, 1),
        arrayOf(TiffTagDataType.ASCII, 1, 2),
        arrayOf(TiffTagDataType.SHORT, 2, 3),
        arrayOf(TiffTagDataType.LONG, 4, 4),
        arrayOf(TiffTagDataType.RATIONAL, 8, 5),
        arrayOf(TiffTagDataType.SBYTE, 1, 6),
        arrayOf(TiffTagDataType.UNDEFINED, 1, 7),
        arrayOf(TiffTagDataType.SSHORT, 2, 8),
        arrayOf(TiffTagDataType.SLONG, 4, 9),
        arrayOf(TiffTagDataType.SRATIONAL, 8, 10),
        arrayOf(TiffTagDataType.FLOAT, 4, 11),
        arrayOf(TiffTagDataType.DOUBLE, 8, 12),
      )
  }
}
