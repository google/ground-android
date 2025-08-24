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
package org.groundplatform.android.data.local.room.converter

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.collections.immutable.toPersistentMap
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object LoiPropertiesMapConverter {
  @TypeConverter
  fun toString(properties: Map<String, Any>): String {
    var jsonString = ""
    try {
      jsonString = JSONObject(properties).toString()
    } catch (e: JSONException) {
      Timber.e(e, "Error building JSON")
    }
    return jsonString
  }

  @TypeConverter
  fun fromString(jsonString: String): Map<String, Any> {
    var map = mutableMapOf<String, Any>()
    try {
      val type = object : TypeToken<Map<String, Any>>() {}.type
      map = Gson().fromJson(jsonString, type)
    } catch (e: JsonSyntaxException) {
      Timber.e(e, "Error parsing JSON string")
    }
    return map.toPersistentMap()
  }
}
