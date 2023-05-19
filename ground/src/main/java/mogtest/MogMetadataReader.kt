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
import timber.log.Timber

object MogMetadataReader {
  fun readMetadata(stream: InputStream): List<Map<TiffTag, Any?>> {
    val bytes = IOUtils.streamBytes(stream)
    val reader = ByteReader(bytes)
    return readMetadata(reader)
  }

  private fun readMetadata(reader: ByteReader): List<Map<TiffTag, Any?>> {

    // Read the 2 bytes of byte order
    var byteOrderString: String? = null
    byteOrderString = reader.readString(2)

    // Determine the byte order
    var byteOrder: ByteOrder? = null
    byteOrder =
      when (byteOrderString) {
        TiffConstants.BYTE_ORDER_LITTLE_ENDIAN -> ByteOrder.LITTLE_ENDIAN
        TiffConstants.BYTE_ORDER_BIG_ENDIAN -> ByteOrder.BIG_ENDIAN
        else -> error("Invalid byte order: $byteOrderString")
      }
    reader.byteOrder = byteOrder

    // Validate the TIFF file identifier
    val tiffIdentifier = reader.readUnsignedShort()
    if (tiffIdentifier != TiffConstants.FILE_IDENTIFIER) {
      error("Invalid file identifier, not a TIFF")
    }

    // Get the offset in bytes of the first image file directory (IFD)
    val byteOffset = reader.readUnsignedInt()

    // Get the TIFF Image
    return readIfds(reader, byteOffset)
  }

  private fun readIfds(reader: ByteReader, initialByteOffset: Long): List<Map<TiffTag, Any?>> {
    var byteOffset = initialByteOffset
    val ifdEntries = mutableListOf<Map<TiffTag, Any?>>()
    while (byteOffset != 0L) {
      readIfdEntries(reader, byteOffset)
      ifdEntries.add(readIfdEntries(reader, byteOffset))
      byteOffset = reader.readUnsignedInt()
    }
    return ifdEntries
  }

  private fun readIfdEntries(reader: ByteReader, byteOffset: Long): Map<TiffTag, Any?> {
    // Set the next byte to read from
    reader.setNextByte(byteOffset)

    val entries = hashMapOf<TiffTag, Any?>()

    // Read the number of directory entries
    val numDirectoryEntries = reader.readUnsignedShort()

    // Read each entry and the values
    for (entryCount in 0 until numDirectoryEntries) {

      // Read the field tag, field type, and type count
      val fieldTagValue = reader.readUnsignedShort()
      val fieldTag: TiffTag? = TiffTag.byId(fieldTagValue)
      val tagTypeId = reader.readUnsignedShort()
      val dataType: TiffTagDataType = TiffTagDataType.byId(tagTypeId)
      val count = reader.readUnsignedInt()

      // Save off the next byte to read location
      val nextByte = reader.nextByte

      Timber.e("Pos before values: ${reader.nextByte}")

      // Read the field values
      val values = readFieldValues(reader, fieldTag, dataType, count)

      // Create and add a file directory if the tag is recognized.
      if (fieldTag != null) {
        entries[fieldTag] = values
      }

      // Restore the next byte to read location
      reader.setNextByte((nextByte + 4).toLong())
    }
    return entries
  }

  private fun readFieldValues(
    reader: ByteReader,
    fieldTag: TiffTag?,
    dataType: TiffTagDataType,
    count: Long
  ): Any? {
    // Large values aren't stored inline, so we store a reference for retrieval in a separate
    // request.
    val fieldSize = dataType.bytes * count
    if (fieldSize > 4) {
      val valueOffset = reader.readUnsignedInt()
      return LongRange(valueOffset, valueOffset + fieldSize)
    }

    val valuesList = readValues(reader, dataType, count)

    // Get the single or array values
    return if (
      count == 1L &&
        fieldTag != null &&
        !fieldTag.isArray &&
        !(dataType == RATIONAL || dataType == SRATIONAL)
    ) {
      valuesList[0]
    } else {
      valuesList
    }
  }

  /**
   * Get the directory entry values
   *
   * @param reader byte reader
   * @param dataType field type
   * @param count type count
   * @return values
   */
  fun readValues(
    reader: ByteReader,
    dataType: TiffTagDataType,
    count: Long
  ): List<Any?> {
    var values: MutableList<Any?> = ArrayList()
    Timber.e("TypeCount: $count")
    // Use UInt and repeat here instead.
    for(i in 1..count) {
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
