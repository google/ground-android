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

enum class TiffTagDataType(
  /** Number of bytes per field value. */
  val bytes: Int
) {
  /** 8-bit unsigned integer */
  BYTE(1),

  /** 8-bit byte that contains a 7-bit ASCII code; the last byte must be NUL (binary zero) */
  ASCII(1),

  /** 16-bit (2-byte) unsigned integer */
  SHORT(2),

  /** 32-bit (4-byte) unsigned integer */
  LONG(4),

  /** Two LONGs: the first represents the numerator of a fraction; the second, the denominator */
  RATIONAL(8),

  /** An 8-bit signed (twos-complement) integer */
  SBYTE(1),

  /** An 8-bit byte that may contain anything, depending on the definition of the field */
  UNDEFINED(1),

  /** A 16-bit (2-byte) signed (twos-complement) integer */
  SSHORT(2),

  /** A 32-bit (4-byte) signed (twos-complement) integer */
  SLONG(4),

  /** Two SLONG’s: the first represents the numerator of a fraction, the second the denominator */
  SRATIONAL(8),

  /** Single precision (4-byte) IEEE format */
  FLOAT(4),

  /** Double precision (8-byte) IEEE format */
  DOUBLE(8);
  /**
   * Get the number of bytes per value
   *
   * @return number of bytes
   */
  val id: Int
    get() = ordinal + 1

  companion object {
    fun byId(id: Int): TiffTagDataType =
      try {
        values()[id - 1]
      } catch (e: ArrayIndexOutOfBoundsException) {
        error("Unknown field type value $id")
      }
  }
}
