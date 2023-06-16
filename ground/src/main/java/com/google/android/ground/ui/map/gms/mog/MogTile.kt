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

import com.google.android.gms.maps.model.Tile

/** Metadata and image data for a single tile. */
class MogTile(val metadata: MogTileMetadata, val data: ByteArray) {
  private val jpegTablesData =
    metadata.jpegTables
      .drop(2) // Drop leading SOI.
      .dropLast(2) // Drop trailing EOI.
      .toByteArray()

  fun toGmsTile(): Tile = Tile(metadata.width, metadata.height, buildJfifFile())

  private fun buildJfifFile(): ByteArray =
    START_OF_IMAGE +
      app0Segment() +
      jpegTablesData +
      data.drop(2) + // Drop leading SOI.
      END_OF_IMAGE

  /** Build "Application Segment 0" section of header. */
  private fun app0Segment() =
    APP0_MARKER +
      APP0_MIN_LEN.toByteArray() +
      JFIF_IDENTIFIER.toNullTerminatedByteArray() +
      JFIF_MAJOR_VERSION.toByte() +
      JFIF_MINOR_VERSION.toByte() +
      NO_DENSITY_UNITS.toByte() +
      metadata.width.toShort().toByteArray() +
      metadata.height.toShort().toByteArray() +
      byteArrayOf(0, 0) // Dimensions of empty thumbnail.
}

/* Circumference of the Earth (m) */
private val START_OF_IMAGE = byteArrayOf(0xFF, 0xD8)
private val APP0_MARKER = byteArrayOf(0xFF, 0xE0)
// Marker segment length with no thumbnails.
private const val APP0_MIN_LEN: Short = 16
private const val JFIF_IDENTIFIER = "JFIF"
private const val JFIF_MAJOR_VERSION = 1
private const val JFIF_MINOR_VERSION = 2
private const val NO_DENSITY_UNITS = 0
private val END_OF_IMAGE = byteArrayOf(0xFF, 0xD9)

private fun byteArrayOf(vararg elements: Int) = elements.map(Int::toByte).toByteArray()

private fun Short.toByteArray() = byteArrayOf(this.toInt().shr(8).toByte(), this.toByte())

private fun String.toNullTerminatedByteArray() = this.toByteArray() + 0x00.toByte()
