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
package org.groundplatform.domain.model.imagery

import kotlin.test.Test
import kotlin.test.assertEquals

class TiffTagDataTypeTest {

  @Test
  fun `data types have expected size and position`() {
    val expected =
      listOf(
        Triple(TiffTagDataType.BYTE, 1, 1),
        Triple(TiffTagDataType.ASCII, 1, 2),
        Triple(TiffTagDataType.SHORT, 2, 3),
        Triple(TiffTagDataType.LONG, 4, 4),
        Triple(TiffTagDataType.RATIONAL, 8, 5),
        Triple(TiffTagDataType.SBYTE, 1, 6),
        Triple(TiffTagDataType.UNDEFINED, 1, 7),
        Triple(TiffTagDataType.SSHORT, 2, 8),
        Triple(TiffTagDataType.SLONG, 4, 9),
        Triple(TiffTagDataType.SRATIONAL, 8, 10),
        Triple(TiffTagDataType.FLOAT, 4, 11),
        Triple(TiffTagDataType.DOUBLE, 8, 12),
      )

    for ((dataType, sizeInBytes, position) in expected) {
      assertEquals(sizeInBytes, dataType.sizeInBytes, "sizeInBytes for $dataType")
      assertEquals(dataType, TiffTagDataType.byId(position), "byId($position)")
    }
  }
}
