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
package org.groundplatform.android.persistence.local.room.converter

import android.util.Base64
import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.groundplatform.android.model.geometry.Geometry
import org.groundplatform.android.persistence.local.room.entity.GeometryWrapper
import org.json.JSONException
import timber.log.Timber

object GeometryWrapperTypeConverter {

  @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
  @TypeConverter
  fun toByteArray(geometryWrapper: GeometryWrapper?): ByteArray =
    geometryWrapper?.getGeometry().let { ProtoBuf.encodeToByteArray(it) }

  @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
  @TypeConverter
  fun fromByteArray(jsonString: ByteArray?): GeometryWrapper? =
    try {
      jsonString?.let { GeometryWrapper.fromGeometry(ProtoBuf.decodeFromByteArray(it)) }
    } catch (e: JSONException) {
      Timber.d(e, "Invalid Geometry in db")
      null
    }

  fun toString(geometry: Geometry?): String = toString(GeometryWrapper.fromGeometry(geometry))

  fun toString(geometryWrapper: GeometryWrapper?): String =
    Base64.encodeToString(toByteArray(geometryWrapper), Base64.DEFAULT)

  fun fromString(byteArrayString: String): GeometryWrapper? =
    fromByteArray(Base64.decode(byteArrayString, Base64.DEFAULT))
}
