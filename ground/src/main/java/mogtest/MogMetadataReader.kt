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

import java.io.InputStream
import java.nio.ByteOrder
import java.util.*
import mogtest.TiffTagDataType.*

class MogMetadataReader(stream: InputStream) {
  private val bytes = IOUtils.streamBytes(stream)
  private val reader = ByteReader(bytes)

  // TODO: Refactor Map into its own class.
  fun readImageFileDirectories(): List<Map<TiffTag, Any?>> {
    reader.byteOrder =
      when (val str = reader.readString(2)) {
        TiffConstants.BYTE_ORDER_LITTLE_ENDIAN -> ByteOrder.LITTLE_ENDIAN
        TiffConstants.BYTE_ORDER_BIG_ENDIAN -> ByteOrder.BIG_ENDIAN
        else -> error("Invalid byte order: $str")
      }

    // TODO: Add support for BigTIFF.
    if (reader.readUnsignedShort() != TiffConstants.FILE_IDENTIFIER) {
      error("Invalid or unsupported TIFF file identifier")
    }

    // Get the TIFF Image
    return readIfds()
  }

  private fun readIfds(): List<Map<TiffTag, Any?>> {
    val ifdEntries = mutableListOf<Map<TiffTag, Any?>>()
    while (true) {
      val byteOffset = reader.readUnsignedInt()
      if (byteOffset == 0L) break
      reader.setOffset(byteOffset)
      val entries = readIfdEntries()
      ifdEntries.add(entries)
    }
    return ifdEntries
  }

  private fun readIfdEntries(): Map<TiffTag, Any?> {
    val entries = hashMapOf<TiffTag, Any?>()
    val entryCount = reader.readUnsignedShort()
    repeat(entryCount) {
      val fieldTagValue = reader.readUnsignedShort()
      val fieldTag: TiffTag? = TiffTag.byId(fieldTagValue)
      val tagTypeId = reader.readUnsignedShort()
      val dataType: TiffTagDataType = TiffTagDataType.byId(tagTypeId)
      val valueCount = reader.readUnsignedInt().toInt()

      val valueOffset = reader.offset

      val values = readFieldValues(fieldTag, dataType, valueCount)

      // Only store recognized fields.
      if (fieldTag != null) {
        entries[fieldTag] = values
      }

      // Scan back to next value in case we went off reading values which weren't inline.
      reader.setOffset((valueOffset + 4).toLong())
    }
    return entries
  }

  private fun readFieldValues(
    fieldTag: TiffTag?,
    dataType: TiffTagDataType,
    valueCount: Int
  ): Any? {
    val fieldSize = dataType.bytes * valueCount
    // Larger values aren't stored inline. Instead, a pointer to the offset of the actual values
    // is stored.
    if (fieldSize > 4) {
      val valueOffset = reader.readUnsignedInt()
      reader.setOffset(valueOffset)
    }

    val valuesList = readValues(dataType, valueCount)

    // Get the single or array values
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

  fun readValues(dataType: TiffTagDataType, valueCount: Int): List<Any?> {
    var values: MutableList<Any?> = ArrayList()
    repeat(valueCount) {
      when (dataType) {
        ASCII -> values.add(reader.readString(1))
        BYTE,
        UNDEFINED -> values.add(reader.readUnsignedByte())
        SBYTE -> values.add(reader.readByte())
        SHORT -> values.add(reader.readUnsignedShort())
        SSHORT -> values.add(reader.readShort())
        LONG -> values.add(reader.readUnsignedInt())
        SLONG -> values.add(reader.readInt())
        RATIONAL -> {
          values.add(reader.readUnsignedInt())
          values.add(reader.readUnsignedInt())
        }
        SRATIONAL -> {
          values.add(reader.readInt())
          values.add(reader.readInt())
        }
        FLOAT -> values.add(reader.readFloat())
        DOUBLE -> values.add(reader.readDouble())
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
