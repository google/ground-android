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

import com.google.common.io.LittleEndianDataInputStream
import java.io.DataInput
import java.io.DataInputStream
import java.nio.charset.StandardCharsets
import java.util.*
import org.groundplatform.android.ui.map.gms.mog.TiffTagDataType.*

private const val NULL_CHAR = 0.toChar()

// TODO: Add unit tests.
// Issue URL: https://github.com/google/ground-android/issues/1596
/** Instances of this class are not thread-safe. */
class MogMetadataReader(private val seekable: SeekableInputStream) {
  private lateinit var dataInput: DataInput

  // TODO: Refactor Map into its own class.
  // Issue URL: https://github.com/google/ground-android/issues/2915
  fun readImageFileDirectories(): List<Map<TiffTag, Any?>> {
    val byteOrderCode = readByteOrderString()
    dataInput = createDataInput(byteOrderCode)

    // TODO: Add support for BigTIFF.
    // Issue URL: https://github.com/google/ground-android/issues/2914
    val fileIdentifier = dataInput.readUnsignedShort()
    validateFileIdentifier(fileIdentifier)

    val ifds = mutableListOf<Map<TiffTag, Any?>>()
    while (true) {
      val byteOffset = dataInput.readUnsignedInt()
      if (byteOffset == 0L) break
      seekable.seek(byteOffset.toInt())
      val ifd = readImageFileDirectory()
      // We don't support masks, so only include RGB images.
      if (hasRgbImageData(ifd)) {
        ifds.add(ifd)
      }
    }
    return ifds
  }

  private fun hasRgbImageData(ifd: Map<TiffTag, Any?>) =
    (ifd[TiffTag.PhotometricInterpretation] as Int).and(PHOTOMETRIC_INTERPRETATION_RGB) != 0

  private fun createDataInput(byteOrderCode: String): DataInput =
    when (byteOrderCode) {
      BYTE_ORDER_LITTLE_ENDIAN -> LittleEndianDataInputStream(seekable)
      BYTE_ORDER_BIG_ENDIAN -> DataInputStream(seekable)
      else -> error("Invalid byte order: $byteOrderCode")
    }

  private fun validateFileIdentifier(fileIdentifier: Int) {
    if (fileIdentifier != TIFF_FILE_IDENTIFIER) {
      error("Invalid or unsupported TIFF file identifier")
    }
  }

  private fun readByteOrderString(): String {
    val bytes = ByteArray(2)
    seekable.read(bytes, 0, 2)
    return String(bytes, StandardCharsets.US_ASCII)
  }

  private fun readImageFileDirectory(): Map<TiffTag, Any?> {
    val entries = hashMapOf<TiffTag, Any?>()
    val entryCount = dataInput.readUnsignedShort()
    repeat(entryCount) {
      val tag: TiffTag? = TiffTag.byId[dataInput.readUnsignedShort()]
      val dataType: TiffTagDataType = TiffTagDataType.byId(dataInput.readUnsignedShort())
      val dataCount = dataInput.readUnsignedInt().toInt()

      seekable.mark()

      val values = readTagData(tag, dataType, dataCount)

      // Skip unrecognized TIFF tags.
      if (tag != null) {
        entries[tag] = values
      }

      seekable.reset()
      seekable.skip(4)
    }
    return entries
  }

  private fun readTagData(fieldTag: TiffTag?, dataType: TiffTagDataType, valueCount: Int): Any? {
    if (fieldTag == null) return null
    val fieldSize = dataType.sizeInBytes * valueCount
    // Larger values aren't stored inline. Instead, a pointer to the position of the actual values
    // is stored.
    if (fieldSize > 4) {
      val dataOffset = dataInput.readUnsignedInt()
      seekable.seek(dataOffset.toInt())
    }

    val valuesList = readTagValues(dataType, valueCount)

    return if (fieldTag.isArray) {
      valuesList
    } else {
      valuesList.firstOrNull()
    }
  }

  private fun readTagValues(dataType: TiffTagDataType, valueCount: Int): List<Any?> {
    val values: MutableList<Any?> = ArrayList()
    repeat(valueCount) { values.add(readTagValue(dataType)) }

    return if (dataType == ASCII) {
      tiffAsciiValuesToStringList(values)
    } else {
      values
    }
  }

  private fun readTagValue(dataType: TiffTagDataType): Any =
    when (dataType) {
      ASCII -> dataInput.readChar()
      BYTE,
      UNDEFINED -> dataInput.readUnsignedByte()
      SBYTE -> dataInput.readByte()
      SHORT -> dataInput.readUnsignedShort()
      SSHORT -> dataInput.readShort()
      LONG -> dataInput.readUnsignedInt()
      SLONG -> dataInput.readInt()
      FLOAT -> dataInput.readFloat()
      DOUBLE -> dataInput.readDouble()
      else -> throw UnsupportedOperationException("Unsupported tag type $dataType")
    }

  /** Returns the ASCII TIFF field values as a string of lists, splitting strings by `null`. */
  private fun tiffAsciiValuesToStringList(values: List<Any?>): List<String> =
    values.map { it?.toString() ?: NULL_CHAR }.joinToString("").split(NULL_CHAR)
}

fun DataInput.readUnsignedInt(): Long = readInt().toLong() and 0xffffffffL

/** Little Endian byte order string. */
const val BYTE_ORDER_LITTLE_ENDIAN = "II"

/** Big Endian byte order string. */
const val BYTE_ORDER_BIG_ENDIAN = "MM"

/** TIFF file Identifier. */
const val TIFF_FILE_IDENTIFIER = 42

const val PHOTOMETRIC_INTERPRETATION_RGB = 2
