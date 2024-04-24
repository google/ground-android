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
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

typealias FirestoreKey = String
typealias FirestoreValue = Any
typealias Message = GeneratedMessageLite<*, *>
typealias MessageFieldName = String
typealias MessageFieldValue = Any
typealias MessageMap = MapFieldLite<Any, Any>

// TODO: Stop modifying immutable object, use builder.instance instead.
fun <T : Message> DocumentSnapshot.copyInto(message: T): T {
  val map = data
  // TODO: set this on the Message instead of modifying the data map.
  map?.set("id", id)
  map?.copyInto(message)
  return message
}

fun Map<String, Any>.copyInto(message: Message) {
  forEach { (key: FirestoreKey, value: FirestoreValue) ->
    val messageFieldName = key.toMessageFieldName()
    try {
      val messageField = message.getField(messageFieldName)
      val messageMapValueType = message.getMapValueType(messageFieldName)
      val messageFieldValue =
        value.toMessageFieldValue(messageField.type.kotlin, messageMapValueType)
      message.set(
        messageField,
        messageFieldValue
      )
    } catch (e: IllegalArgumentException) {
      Timber.v(e, "Can't set incompatible value on ${message.javaClass}: $messageFieldName=$value")
    } catch (e: ClassCastException) {
      Timber.v(e, "Can't set incompatible type on ${message.javaClass}:  $messageFieldName=$value")
    } catch (e: NoSuchFieldException) {
      Timber.v(e, "Skipping unknown field in ${message.javaClass}: $messageFieldName=$value")
    }
  }
}

private fun FirestoreKey.toMessageFieldName(): MessageFieldName = this

private fun Message.getMapValueType(key: FirestoreKey): KClass<*>? =
  javaClass.declaredMethods.find {
    it.name == key.toMapValueGetterMethodName()
  }?.returnType?.kotlin


private fun FirestoreKey.toMapValueGetterMethodName(): String =
  "get${
    replaceFirstChar {
      if (it.isLowerCase()) it.uppercaseChar() else it.lowercaseChar()
    }
  }OrDefault"


private fun Message.getField(name: MessageFieldName): Field =
  javaClass.getDeclaredField(name + "_")


private fun Message.set(
  field: Field,
  value: Any?
) {
  field.isAccessible = true
  field.set(this, value)
  field.isAccessible = false
}

fun Any.toMessageFieldValue(
  targetType: KClass<*>,
  mapValueType: KClass<*>?
): MessageFieldValue =
  if (targetType == String::class) {
    this as String
  } else if (targetType.isSubclassOf(Map::class)) {
    (this as Map<*, *>).toMessageFieldValue(mapValueType!!)
  } else if (targetType.isSubclassOf(GeneratedMessageLite::class)) {
    Timber.e("!!!! TODO: Impl copy to $targetType")
    "TODO"
  } else {
    ""
    // TODO: Handle maps, arrays, GeoPoint, and other types.
    // throw UnsupportedOperationException()
  }

private fun newMessageMap(): MessageMap =
  MapFieldLite.emptyMapField<Any, Any>().mutableCopy()

private fun messageMapOf(data: Map<*, *>): MessageMap =
  newMessageMap().also {
    it.putAll(data)
    it.makeImmutable()
  }

fun Map<*, *>.toMessageFieldValue(valueType: KClass<*>): MessageMap =
  messageMapOf(mapValues { (_, v) -> v?.toMessageFieldValue(valueType, null) }
    .filterValues { v -> v != null }
  )
