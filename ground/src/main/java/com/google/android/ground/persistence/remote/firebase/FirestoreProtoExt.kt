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

package com.google.android.ground.persistence.remote.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.protobuf.GeneratedMessageLite
import com.google.protobuf.MapFieldLite
import timber.log.Timber

fun <T : GeneratedMessageLite<*, *>> DocumentSnapshot.copyInto(proto: T?): T? {
  proto ?: return null
  val map = data
  // TODO: Replace `id` with `uuid`?
  map?.set("id", id)
  map?.forEach { (key, value) ->
    try {
      val field = proto::class.java.getDeclaredField(key + "_")
      field.isAccessible = true
      // TODO: Catch IllegalArgumentException and ClassCastException thrown when value is wrong
      // type.
      val convertedValue = convertValue(value, field.type)
      field.set(proto, convertedValue)
      // TODO: Handle maps, arrays, GeoPoint, and other types.

      field.isAccessible = false
    } catch (e: NoSuchFieldException) {
      Timber.v("Skipping unknown field: $key")
    } catch (e: IllegalArgumentException) {
      // TODO: Add expected and actual
      Timber.v("Skipping field with wrong type: $key")
    }
  }
  return proto
}

fun convertValue(value: Any, type: Class<*>): Any =
  if (type.isAssignableFrom(String::class.java)) {
    value as String
  } else if (Map::class.java.isAssignableFrom(type)) {
    convertMapValue(value as Map<*, *>)
  } else {
    ""
    // throw UnsupportedOperationException()
  }

fun convertValue(value: Any?) = value?.let { convertValue(it, it.javaClass) }

fun convertMapValue(mapValue: Map<*, *>): MapFieldLite<Any, Any?> {
  val newMap = MapFieldLite.emptyMapField<Any?, Any?>().mutableCopy()
  mapValue.entries.forEach { (k, v) -> newMap[k as String] = convertValue(v) }
  return newMap
}
