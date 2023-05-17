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

object TiffConstants {
  /** Little Endian byte order string. */
  const val BYTE_ORDER_LITTLE_ENDIAN = "II"

  /** Big Endian byte order string. */
  const val BYTE_ORDER_BIG_ENDIAN = "MM"

  /** TIFF file Identifier. */
  const val FILE_IDENTIFIER = 42

  /** Image File Directory header / number of entries bytes. */
  const val IFD_HEADER_BYTES = 2

  /** Image File Directory offset to the next IFD bytes. */
  const val IFD_OFFSET_BYTES = 4

  /** Image File Directory entry bytes. */
  const val IFD_ENTRY_BYTES = 12

  const val PHOTOMETRIC_INTERPRETATION_RGB = 2
}
