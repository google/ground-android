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

import com.google.protobuf.GeneratedMessageLite
import com.google.protobuf.Internal.EnumLite
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible
import timber.log.Timber

/** A key used in a document or a nested object in Firestore. */
internal typealias FirestoreKey = String

/** A value used in a document or a nested object in Firestore. */
internal typealias FirestoreValue = Any

/** A nested object, aka map value in a Firestore document. */
internal typealias FirestoreMap = Map<FirestoreKey, FirestoreValue>

internal typealias FirestoreMapEntry = Map.Entry<FirestoreKey, FirestoreValue>

/** A Protocol Buffer message instance. */
internal typealias Message = GeneratedMessageLite<*, *>

private const val BIT_FIELD_PROPERTY_PREFIX = "bitField"

private const val FIELD_NUMBER_CONST_SUFFIX = "_FIELD_NUMBER"

private const val ONE_OF_CASE_PROPERTY_SUFFIX = "Case_"

/** Lower snake case name of an individual field in a message instance. */
internal typealias MessageFieldName = String

/** The field number of an individual field in a message instance. */
internal typealias MessageFieldNumber = Int

/** An individual field value in a message instance. */
internal typealias MessageValue = Any

internal typealias MessageField = Pair<MessageFieldName, MessageValue>

/** The value of a map field in a message instance. */
internal typealias MessageMap = Map<*, *>

fun <T : MessageBuilder> KClass<T>.getMapValueType(key: String): KClass<*> {
  val mapValueGetterName = key.toMessageMapGetterMethodName()
  val mapValueGetterMethod = java.declaredMethods.find { it.name == mapValueGetterName }
  return mapValueGetterMethod?.returnType?.kotlin
    ?: throw NoSuchMethodError("$mapValueGetterName method")
}

@Suppress("StringLiteralDuplication", "SwallowedException")
fun <T : MessageBuilder> KClass<T>.getFieldTypeByName(fieldName: String): KClass<*> =
  try {
    java.getDeclaredMethod("get${fieldName.toUpperCamelCase()}").returnType?.kotlin
      ?: throw UnsupportedOperationException("Getter not found for field $fieldName")
  } catch (e: NoSuchMethodException) {
    // Could be a list type instead. Check for a `getFieldList()` method.
    java.getDeclaredMethod("get${fieldName.toUpperCamelCase()}List").returnType?.kotlin
      ?: throw UnsupportedOperationException("Getter not found for field $fieldName")
  }

fun <T : MessageBuilder> KClass<T>.getListElementFieldTypeByName(fieldName: String): KClass<*> =
  // Each list field has a getter with an index.
  java.getDeclaredMethod("get${fieldName.toUpperCamelCase()}", Int::class.java).returnType?.kotlin
    ?: throw UnsupportedOperationException("Getter not found for field $fieldName")

private fun MessageBuilder.getSetterByFieldName(fieldName: String): KFunction<*> =
  // Message fields generated two setters; ignore the Builder's setter in favor of the
  // message setter.
  this::class.declaredFunctions.find {
    it.name == "set${fieldName.toUpperCamelCase()}" && !it.parameters[1].type.isBuilder()
  } ?: throw UnsupportedOperationException("Setter not found for field $fieldName")

private fun MessageBuilder.getAddAllByFieldName(fieldName: String): KFunction<*> =
  // Message fields generated two setters; ignore the Builder's setter in favor of the
  // message setter.
  this::class.declaredFunctions.find {
    it.name == "addAll${fieldName.toUpperCamelCase()}" && !it.parameters[1].type.isBuilder()
  } ?: throw UnsupportedOperationException("addAll not found for field $fieldName")

private fun KType.isBuilder() =
  (classifier as KClass<*>).isSubclassOf(GeneratedMessageLite.Builder::class)

private fun MessageBuilder.getPutAllByFieldName(fieldName: String): KFunction<*> =
  this::class.declaredFunctions.find { it.name == "putAll${fieldName.toUpperCamelCase()}" }
    ?: throw UnsupportedOperationException("Putter not found for field $fieldName")

fun <T : Message> KClass<T>.newBuilderForType() =
  java.getDeclaredMethod("newBuilder").invoke(null) as MessageBuilder

@Suppress("StringLiteralDuplication")
fun MessageBuilder.setOrLog(fieldName: MessageFieldName, value: MessageValue) {
  try {
    set(fieldName, value)
  } catch (e: Throwable) {
    Timber.e(e, "Skipping incompatible value in ${javaClass}: $fieldName=$value")
  }
}

@Suppress("StringLiteralDuplication")
fun MessageBuilder.addAllOrLog(fieldName: MessageFieldName, value: MessageValue) {
  try {
    addAll(fieldName, value)
  } catch (e: Throwable) {
    Timber.e(e, "Skipping incompatible value in ${javaClass}: $fieldName=$value")
  }
}

fun <T : Message> KClass<T>.getFieldName(fieldNumber: MessageFieldNumber): MessageFieldName =
  getStaticFields()
    .find { it.name.endsWith(FIELD_NUMBER_CONST_SUFFIX) && it.get(null) == fieldNumber }
    ?.name
    ?.removeSuffix(FIELD_NUMBER_CONST_SUFFIX)
    ?.lowercase() ?: throw IllegalArgumentException("Field $fieldNumber not found in $java")

fun <T : Message> KClass<T>.getFieldNumber(fieldName: MessageFieldName): MessageFieldNumber? =
  getStaticFields().find { it.name == fieldName.toFieldNumberConstantName() }?.get(null)
    as? MessageFieldNumber

fun Message.getSetOneOfFieldNumber(fieldName: String): MessageFieldNumber? {
  val casePropertyName = fieldName.toCamelCase() + ONE_OF_CASE_PROPERTY_SUFFIX
  val caseProperty = this::class.declaredMemberProperties.find { it.name == casePropertyName }
  return caseProperty?.let { get(it) } as? MessageFieldNumber
}

fun Message.get(property: KProperty<*>): Any? {
  property.isAccessible = true
  val value = property.call(this)
  property.isAccessible = false
  return value
}

private fun String.toFieldNumberConstantName(): String = uppercase() + FIELD_NUMBER_CONST_SUFFIX

@Suppress("UNCHECKED_CAST")
fun <T : MessageBuilder> KClass<T>.getMessageClass() =
  java.enclosingClass!!.kotlin as KClass<Message>

private fun KClass<*>.getStaticFields() =
  java.declaredFields.filter { Modifier.isStatic(it.modifiers) }

fun MessageBuilder.putAllOrLog(fieldName: MessageFieldName, value: MessageMap) {
  try {
    putAll(fieldName, value)
  } catch (e: Throwable) {
    Timber.e(e, "Skipping incompatible value in ${javaClass}: $fieldName=$value")
  }
}

private fun String.toMessageMapGetterMethodName() = "get${toUpperCamelCase()}OrDefault"

private fun MessageFieldName.toCamelCase(): String {
  val pattern = "_[a-z]".toRegex()
  return replace(pattern) { it.value.last().uppercase() }
}

private fun String.toUpperCamelCase(): String =
  toCamelCase().replaceFirstChar { it.uppercaseChar() }

private fun MessageBuilder.set(fieldName: MessageFieldName, value: MessageValue) {
  getSetterByFieldName(fieldName).call(this, value)
}

private fun MessageBuilder.addAll(fieldName: MessageFieldName, value: MessageValue) {
  getAddAllByFieldName(fieldName).call(this, value)
}

private fun MessageBuilder.putAll(fieldName: MessageFieldName, value: MessageMap) {
  getPutAllByFieldName(fieldName).call(this, value)
}

fun <T : Message> KClass<T>.getFieldProperties(): List<KProperty<*>> =
  declaredMemberProperties.filter { it.isMessageFieldProperty() }

private fun KProperty<*>.isMessageFieldProperty(): Boolean =
  name.endsWith("_") &&
    !name.startsWith(BIT_FIELD_PROPERTY_PREFIX) &&
    !name.endsWith(ONE_OF_CASE_PROPERTY_SUFFIX)

fun String.toSnakeCase() = replace(Regex("[A-Z]")) { "_${it.value}" }.lowercase()

private fun getEnumValues(enumClass: KClass<out Enum<*>>): List<Enum<*>> =
  enumClass.java.enumConstants.toList()

@Suppress("UNCHECKED_CAST")
fun <T : EnumLite> KClass<T>.findByNumber(number: Int): T? {
  require(isSubclassOf(Enum::class))
  return getEnumValues(this as KClass<Enum<*>>).find { (it as EnumLite).number == number } as? T
}
