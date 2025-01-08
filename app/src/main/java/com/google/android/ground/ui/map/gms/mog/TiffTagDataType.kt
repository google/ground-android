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

/** Not all types are used by this implementation, but all are included here for completeness. */
enum class TiffTagDataType(
  /** Number of bytes occupied by a single value of this type. */
  val sizeInBytes: Int
) {
  /** 8-bit unsigned integer. */
  BYTE(1),

  /** A NUL (0) terminated ASCII string. */
  ASCII(1),

  /** 16-bit (2-byte) unsigned integer. */
  SHORT(2),

  /** 32-bit (4-byte) unsigned integer. */
  LONG(4),

  /** Two LONG values representing the numerator and denominator of a fraction, respectively. */
  RATIONAL(8),

  /** An 8-bit signed (two's complement) integer. */
  SBYTE(1),

  /** An 8-bit value whose interpretation is tag dependent. */
  UNDEFINED(1),

  /** A 16-bit (2-byte) signed (two's complement) integer. */
  SSHORT(2),

  /** A 32-bit (4-byte) signed (two's ccomplement) integer. */
  SLONG(4),

  /** Two SLONG values representing the numerator and denominator of a fraction, respectively. */
  SRATIONAL(8),

  /** Single precision (4-byte) IEEE floating point number. */
  FLOAT(4),

  /** Double precision (8-byte) IEEE floating point number. */
  DOUBLE(8);

  companion object {
    fun byId(id: Int): TiffTagDataType {
      check(id >= 1 && id <= entries.size) { "Unsupported tag data type $id" }
      return entries[id - 1]
    }
  }
}
