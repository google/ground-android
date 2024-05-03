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

typealias FirestoreMap = Map<FirestoreKey, FirestoreValue>

typealias Message = GeneratedMessageLite<*, *>

typealias MessageFieldName = String

typealias MessageValue = Any

typealias MessageMap = MapFieldLite<*, *>

/** TODO: Add note about this function being tightly bound to protobuf lite codegen's impl. */
fun <T : Message> DocumentSnapshot.toMessage(messageType: KClass<T>): T {
  val message = messageType.newInstance()
  message.set("id", id)
  data?.forEach { (key: FirestoreKey, value: FirestoreValue) -> message.set(key, value) }
  return message
}

private fun <T : Message> FirestoreMap.toMessage(messageType: KClass<T>): T {
  val message = messageType.newInstance()
  forEach { (key: FirestoreKey, value: FirestoreValue) -> message.set(key, value) }
  return message
}

private fun FirestoreMap.toMessageMap(mapValueType: KClass<*>): MessageMap {
  val mapField = MessageMap.emptyMapField<Any, Any>().mutableCopy()
  forEach { (key: FirestoreValue, value: FirestoreValue) ->
    mapField[key] = value.toMessageValue(mapValueType)
  }
  mapField.makeImmutable()
  return mapField
}

private fun <T : Message> KClass<T>.newInstance(): T =
  constructors.first().apply { isAccessible = true }.call()

private fun Message.set(key: FirestoreKey, value: FirestoreValue) {
  try {
    val fieldName: MessageFieldName = key
    val field = getFieldByName(fieldName)
    val fieldValue = value.toMessageValue(this, fieldName)
    setPrivate(field, fieldValue)
  } catch (e: Throwable) {
    Timber.e(e, "Skipping incompatible Firestore value. ${javaClass}: $key=$value")
  }
}

private fun Message.getFieldByName(fieldName: String): Field =
  javaClass.getDeclaredField("${fieldName}_")

private fun Message.getMapValueType(key: FirestoreKey): KClass<*> {
  val mapValueGetterName = key.toMapValueGetterMethodName()
  val mapValueGetterMethod = javaClass.declaredMethods.find { it.name == mapValueGetterName }
  return mapValueGetterMethod?.returnType?.kotlin
    ?: throw NoSuchMethodError("$mapValueGetterName method")
}

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

@Suppress("UNCHECKED_CAST")
fun FirestoreValue.toMessageValue(message: Message, fieldName: String): MessageValue {
  val field = message.getFieldByName(fieldName)
  val fieldType = field.type.kotlin
  return if (fieldType.isSubclassOf(Map::class)) {
    (this as FirestoreMap).toMessageMap(message.getMapValueType(fieldName))
  } else {
    toMessageValue(fieldType)
  }
}

@Suppress("UNCHECKED_CAST")
fun FirestoreValue.toMessageValue(targetType: KClass<*>): MessageValue =
  if (targetType == String::class) {
    this as String
  } else if (targetType.isSubclassOf(GeneratedMessageLite::class)) {
    (this as FirestoreMap).toMessage(targetType as KClass<GeneratedMessageLite<*, *>>)
  } else {
    throw UnsupportedOperationException("Unsupported message field type $targetType")
    // TODO: Handle arrays, GeoPoint, int, and other types.
  }
