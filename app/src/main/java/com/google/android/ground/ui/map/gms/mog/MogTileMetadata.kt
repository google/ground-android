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

/** Metadata for the tiles with the specified coordinates provided by the specified MOG. */
data class MogTileMetadata(
  val tileCoordinates: TileCoordinates,
  val width: Int,
  val height: Int,
  val jpegTables: ByteArray,
  val byteRange: LongRange,
  val noDataValue: Int? = null,
) {
  // Default [equals] and [hashCode] behavior doesn't take array members into account. Defend
  // against accidental usage by throwing exception if called.
  @Suppress("detekt:ExceptionRaisedInUnexpectedLocation")
  override fun equals(other: Any?) = throw UnsupportedOperationException()

  @Suppress("detekt:ExceptionRaisedInUnexpectedLocation")
  override fun hashCode() = throw UnsupportedOperationException()
}
