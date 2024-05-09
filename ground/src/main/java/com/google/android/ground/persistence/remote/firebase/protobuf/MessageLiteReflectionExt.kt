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

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import timber.log.Timber

fun KClass<MessageBuilder>.getMapValueType(key: String): KClass<*> {
  val mapValueGetterName = key.toMessageMapGetterMethodName()
  val mapValueGetterMethod = java.declaredMethods.find { it.name == mapValueGetterName }
  return mapValueGetterMethod?.returnType?.kotlin
    ?: throw NoSuchMethodError("$mapValueGetterName method")
}

fun KClass<MessageBuilder>.getFieldTypeByName(fieldName: String): KClass<*> =
  java.getDeclaredMethod("get${fieldName.toSentenceCase()}").returnType?.kotlin
    ?: throw UnsupportedOperationException("Getter not found for field $fieldName")

private fun MessageBuilder.getSetterByFieldName(fieldName: String): KFunction<*> =
  this::class.declaredFunctions.find { it.name == "set${fieldName.toSentenceCase()}" }
    ?: throw UnsupportedOperationException("set*() not found for field $fieldName")

private fun MessageBuilder.getPutAllByFieldName(fieldName: String): KFunction<*> =
  this::class.declaredFunctions.find { it.name == "putAll${fieldName.toSentenceCase()}" }
    ?: throw UnsupportedOperationException("putAll*() not found for field $fieldName")

fun KClass<Message>.newBuilderForType() =
  java.getDeclaredMethod("newBuilder").invoke(null) as MessageBuilder

fun MessageBuilder.setOrLog(fieldName: MessageFieldName, value: MessageValue) {
  try {
    set(fieldName, value)
  } catch (e: Throwable) {
    Timber.e(e, "Skipping incompatible value in ${javaClass}: $fieldName=$value")
  }
}

fun MessageBuilder.putAllOrLog(fieldName: MessageFieldName, value: MessageMap) {
  try {
    putAll(fieldName, value)
  } catch (e: Throwable) {
    Timber.e(e, "Skipping incompatible value in ${javaClass}: $fieldName=$value")
  }
}

private fun String.toMessageMapGetterMethodName() = "get${toSentenceCase()}OrDefault"

private fun String.toSentenceCase() = replaceFirstChar {
  if (it.isLowerCase()) it.uppercaseChar() else it
}

private fun MessageBuilder.set(fieldName: MessageFieldName, value: MessageValue) {
  getSetterByFieldName(fieldName).call(this, value)
}

private fun MessageBuilder.putAll(fieldName: MessageFieldName, value: MessageMap) {
  getPutAllByFieldName(fieldName).call(this, value)
}
