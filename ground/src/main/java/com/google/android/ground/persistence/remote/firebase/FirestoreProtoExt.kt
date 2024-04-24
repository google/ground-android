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
import java.lang.ClassCastException

typealias FirestoreKey = String
typealias FirestoreValue = Any
typealias MessageFieldKey = String
typealias MessageFieldValue = Any
typealias MessageMap = MapFieldLite<Any, Any>

fun <T : GeneratedMessageLite<*, *>> DocumentSnapshot.copyInto(message: T) {
  val map = data
  // TODO: Replace `id` with `uuid`?
  map?.set("id", id)
  map?.forEach { (key: FirestoreKey, value: FirestoreValue) ->
    try {
      message.set(key + "_", value.toMessageFieldValue())
    } catch (e: IllegalArgumentException) {
      Timber.v("Can't set incompatible value on ${message.javaClass}: $key=$value")
    } catch (e: ClassCastException) {
      Timber.v("Can't set incompatible type on ${message.javaClass}:  $key=$value")
    } catch (e: NoSuchFieldException) {
      Timber.v("Skipping unknown field in ${message.javaClass}: $key=$value")
    }
  }
}

private fun GeneratedMessageLite<*, *>.set(
  key: MessageFieldKey,
  value: MessageFieldValue?
) {
  val field = javaClass.getDeclaredField(key + "_")
  field.isAccessible = true
  field.set(value, value)
  field.isAccessible = false
}

fun Any.toMessageFieldValue(type: Class<*>): MessageFieldValue =
  if (type.isAssignableFrom(String::class.java)) {
    this as String
  } else if (Map::class.java.isAssignableFrom(type)) {
    (this as Map<*, *>).toMessageFieldValue()
  } else {
    ""
    // TODO: Handle maps, arrays, GeoPoint, and other types.
    // throw UnsupportedOperationException()
  }

fun Any.toMessageFieldValue() = toMessageFieldValue(javaClass)

private fun newMessageMap(): MessageMap =
  MapFieldLite.emptyMapField<Any, Any>().mutableCopy()

private fun messageMapOf(data: Map<*, *>): MessageMap =
  newMessageMap().also {
    it.putAll(data)
    it.makeImmutable()
  }

fun Map<*, *>.toMessageFieldValue(): MessageMap =
  messageMapOf(mapValues { (_, v) -> v?.toMessageFieldValue() }
    .filterValues { v -> v != null }
  )