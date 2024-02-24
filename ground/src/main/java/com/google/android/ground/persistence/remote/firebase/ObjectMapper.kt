/*
 * Copyright 2024 Google LLC
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

package com.google.android.ground.persistence.remote.firebase

import com.google.ground.shared.schema.GeometryObject
import com.google.ground.shared.schema.MultiPolygonObject
import com.google.ground.shared.schema.PointObject
import com.google.ground.shared.schema.PolygonObject
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import java.lang.reflect.Type
import kotlin.reflect.KClass

class ObjectMapper {
  fun <T : Any> toObject(map: Map<String, Any>, kotlinClass: KClass<T>): T {

    val gson =
      GsonBuilder().registerTypeAdapter(GeometryObject::class.java, GeometryDeserializer()).create()
    val json = map.toJsonElement()
    return gson.fromJson(json, kotlinClass.java)
  }

  private fun Any?.toJsonElement(): JsonElement =
    when (this) {
      null -> JsonNull.INSTANCE
      is JsonElement -> this
      is Number -> JsonPrimitive(this)
      is Boolean -> JsonPrimitive(this)
      is String -> JsonPrimitive(this)
      is Array<*> -> toJsonArray()
      is List<*> -> toJsonArray()
      // TODO: Add annotations for nested arrays.
      is Map<*, *> -> toJsonObject()
      else -> error("Unsupported type ${this.javaClass}")
    }

  private fun List<*>.toJsonArray(): JsonArray {
    val arr = JsonArray(size)
    forEach { arr.add(it.toJsonElement()) }
    return arr
  }

  private fun Array<*>.toJsonArray() = arrayListOf(this).toJsonArray()

  private fun Map<*, *>.toJsonObject(): JsonObject {
    val obj = JsonObject()
    entries.forEach { (k, v) -> obj.add(k.toString(), v.toJsonElement()) }
    return obj
  }
}

class GeometryDeserializer : JsonDeserializer<GeometryObject> {
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ): GeometryObject {
    val type = json.asJsonObject["type"].asString
    val kotlinClass =
      geometryClassesByType[type] ?: throw JsonParseException("Unknown geometry type $type")
    return context.deserialize(json, kotlinClass.java)
  }
}

internal val geometryClassesByType =
  mapOf(
    "Point" to PointObject::class,
    "Polygon" to PolygonObject::class,
    "MultiPolygon" to MultiPolygonObject::class
  )
