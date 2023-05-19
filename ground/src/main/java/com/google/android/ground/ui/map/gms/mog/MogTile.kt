package com.google.android.ground.ui.map.gms.mog

import com.google.android.gms.maps.model.Tile

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
      JFIF_IDENTIFIER.toNulTerminatedByteArray() +
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

private fun String.toNulTerminatedByteArray() = this.toByteArray() + 0x00.toByte()
