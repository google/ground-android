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

import com.google.android.ground.ui.map.gms.mog.TiffTagDataType.*
import com.google.common.io.LittleEndianDataInputStream
import java.io.DataInput
import java.io.DataInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*

/** Instances of this class are not thread-safe. */
class MogMetadataReader(sourceStream: InputStream) {
  private val seekable = SeekableInputStream(sourceStream)
  private lateinit var dataInput: DataInput

  // TODO: Refactor Map into its own class.
  fun readImageFileDirectories(): List<Map<TiffTag, Any?>> {
    val byteOrderCode = readByteOrderString()
    dataInput = createDataInput(byteOrderCode)

    // TODO: Add support for BigTIFF.
    val fileIdentifier = dataInput.readUnsignedShort()
    validateFileIdentifier(fileIdentifier)

    val ifds = mutableListOf<Map<TiffTag, Any?>>()
    while (true) {
      val byteOffset = dataInput.readUnsignedInt()
      if (byteOffset == 0L) break
      seekable.seek(byteOffset.toInt())
      val ifd = readImageFileDirectory()
      // We don't support masks, so only include RGB images.
      if (
        (ifd[TiffTag.PhotometricInterpretation] as Int).and(PHOTOMETRIC_INTERPRETATION_RGB) != 0
      ) {
        ifds.add(ifd)
      }
    }
    return ifds
  }

  private fun createDataInput(byteOrderCode: String): DataInput =
    when (byteOrderCode) {
      BYTE_ORDER_LITTLE_ENDIAN ->
        // TODO: Upgrade to Guava 31.2 once released and remove @Suppress.
        @Suppress("UnstableApiUsage") LittleEndianDataInputStream(seekable)
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

      // Only store recognized fields.
      if (tag != null) {
        entries[tag] = values
      }

      seekable.reset()
      seekable.skip(4)
    }
    return entries
  }

  private fun readTagData(fieldTag: TiffTag?, dataType: TiffTagDataType, valueCount: Int): Any? {
    val fieldSize = dataType.sizeInBytes * valueCount
    // Larger values aren't stored inline. Instead, a pointer to the position of the actual values
    // is stored.
    if (fieldSize > 4) {
      val dataOffset = dataInput.readUnsignedInt()
      seekable.seek(dataOffset.toInt())
    }

    val valuesList = readTagValues(dataType, valueCount)

    return if (
      valueCount == 1 &&
        fieldTag != null &&
        !fieldTag.isArray &&
        !(dataType == RATIONAL || dataType == SRATIONAL)
    ) {
      valuesList[0]
    } else {
      valuesList
    }
  }

  private fun readTagValues(dataType: TiffTagDataType, valueCount: Int): List<Any?> {
    var values: MutableList<Any?> = ArrayList()
    repeat(valueCount) {
      when (dataType) {
        ASCII -> values.add(dataInput.readChar())
        BYTE,
        UNDEFINED -> values.add(dataInput.readUnsignedByte())
        SBYTE -> values.add(dataInput.readByte())
        SHORT -> values.add(dataInput.readUnsignedShort())
        SSHORT -> values.add(dataInput.readShort())
        LONG -> values.add(dataInput.readUnsignedInt())
        SLONG -> values.add(dataInput.readInt())
        RATIONAL -> {
          values.add(dataInput.readUnsignedInt())
          values.add(dataInput.readUnsignedInt())
        }
        SRATIONAL -> {
          values.add(dataInput.readInt())
          values.add(dataInput.readInt())
        }
        FLOAT -> values.add(dataInput.readFloat())
        DOUBLE -> values.add(dataInput.readDouble())
      }
    }

    // If ASCII characters, combine the strings
    if (dataType == ASCII) {
      val stringValues: MutableList<Any?> = ArrayList()
      var stringValue = StringBuilder()
      for (value in values) {
        if (value == null) {
          if (stringValue.isNotEmpty()) {
            stringValues.add(stringValue.toString())
            stringValue = StringBuilder()
          }
        } else {
          stringValue.append(value.toString())
        }
      }
      values = stringValues
    }
    return values
  }
}

fun DataInput.readUnsignedInt(): Long = readInt().toLong() and 0xffffffffL

/** Little Endian byte order string. */
const val BYTE_ORDER_LITTLE_ENDIAN = "II"

/** Big Endian byte order string. */
const val BYTE_ORDER_BIG_ENDIAN = "MM"

/** TIFF file Identifier. */
const val TIFF_FILE_IDENTIFIER = 42

const val PHOTOMETRIC_INTERPRETATION_RGB = 2
