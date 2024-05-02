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

package com.google.android.ground.persistence.remote.firebase.protobuf

import com.google.firebase.firestore.DocumentSnapshot
import com.google.protobuf.GeneratedMessageLite
import com.google.protobuf.MapFieldLite
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible
import timber.log.Timber

typealias FirestoreKey = String

typealias FirestoreValue = Any

typealias Message = GeneratedMessageLite<*, *>

typealias MessageFieldName = String

typealias MessageFieldValue = Any

typealias MessageMap = MapFieldLite<Any, Any>

/** TODO: Add note about this function being tightly bound to protobuf lite codegen's impl. */
fun <T : Message> DocumentSnapshot.toMessage(messageType: KClass<T>): T {
  val message = messageType.newInstance()
  message.set("id", id)
  data?.forEach { (key: FirestoreKey, value: FirestoreValue) ->
    try {
      message.set(key, value)
    } catch (e: IllegalArgumentException) {
      Timber.v(e, "Can't set incompatible value on ${message.javaClass}: $key=$value")
    } catch (e: ClassCastException) {
      Timber.v(e, "Can't set incompatible type on ${message.javaClass}:  $key=$value")
    } catch (e: NoSuchFieldException) {
      Timber.v(e, "Skipping unknown field in ${message.javaClass}: $key=$value")
    }
  }

  return message
}

private fun <T : Message> KClass<T>.newInstance(): T =
  constructors.first().apply { isAccessible = true }.call()

private fun Message.set(key: FirestoreKey, value: FirestoreValue) {
  val fieldName: MessageFieldName = key
  val field = getFieldByName(fieldName)
  //  val fieldValue: MessageFieldValue = toFieldValue(field, value)
  //  val messageMapValueType = getMapValueType(fieldName)
  //  (this as Map<*, *>).toMessageFieldValue(mapValueType!!)
  val fieldType = field.type.kotlin
  val fieldValue =
    if (fieldType.isSubclassOf(Map::class)) null else value.toMessageFieldValue(fieldType)
  setPrivate(field, fieldValue)
}

private fun Message.getFieldByName(fieldName: String): Field =
  javaClass.getDeclaredField("${fieldName}_")

private fun Message.getMapValueType(key: FirestoreKey): KClass<*>? =
  javaClass.declaredMethods.find { it.name == key.toMapValueGetterMethodName() }?.returnType?.kotlin

private fun FirestoreKey.toMapValueGetterMethodName(): String =
  "get${
    replaceFirstChar {
      if (it.isLowerCase()) it.uppercaseChar() else it.lowercaseChar()
    }
  }OrDefault"

private fun Message.setPrivate(field: Field, value: Any?) {
  field.isAccessible = true
  field.set(this, value)
  field.isAccessible = false
}

fun Any.toMessageFieldValue(targetType: KClass<*>): MessageFieldValue =
  if (targetType == String::class) {
    this as String
  } else if (targetType.isSubclassOf(GeneratedMessageLite::class)) {
    Timber.e("!!!! TODO: Impl copy to $targetType")
    "TODO"
  } else {
    ""
    // TODO: Handle maps, arrays, GeoPoint, and other types.
    // throw UnsupportedOperationException()
  }

// private fun newMessageMap(): MessageMap = MapFieldLite.emptyMapField<Any, Any>().mutableCopy()

// private fun messageMapOf(data: Map<*, *>): MessageMap =
//  newMessageMap().also {
//    it.putAll(data)
//    it.makeImmutable()
//  }

// fun Map<*, *>.toMessageFieldValue(valueType: KClass<*>): MessageMap =
//  messageMapOf(
//    mapValues { (_, v) -> v?.toMessageFieldValue(valueType, null) }.filterValues { v -> v != null
// }
//  )
