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

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import timber.log.Timber

/**
 * Returns the map representation of this message ready for serialization in Firestore. The map is
 * keyed by message field numbers represented as strings, while field values are converted to
 * corresponding Firestore data types. Caveats:
 * * Proto field names starting with `bit_field` are not supported.
 * * TODO: Handle ids
 */
fun Message.toFirestoreMap(): FirestoreMap =
  this::class.getFieldProperties().mapNotNull { toFirestoreValue(it) }.toMap()

private fun Message.toFirestoreValue(property: KProperty<*>): Pair<FirestoreKey, FirestoreValue>? {
  try {
    if (!hasValue(property)) return null
    val fieldName = property.name.toMessageFieldName()
    val key = this::class.getFieldNumber(fieldName).toString()
    val value = get(property)?.toFirestoreValue() ?: return null
    return key to value
  } catch (e: Throwable) {
    Timber.v(e, "Skipping property $property")
    return null
  }
}

private fun Message.hasValue(property: KProperty<*>): Boolean {
  val value = get(property)
  return value != null && value != defaultInstanceForType.get(property)
}

private fun Message.get(property: KProperty<*>): Any? {
  property.isAccessible = true
  val value = property.call(this)
  property.isAccessible = false
  return value
}

private fun MessageValue.toFirestoreValue(): FirestoreValue =
  // TODO(#1748): Convert enums and other types.
  when (this) {
    is Message -> toFirestoreMap()
    is Map<*, *> -> mapValues { it.value?.toFirestoreValue() }
    is String,
    is Number -> this
    else -> throw UnsupportedOperationException("Unsupported type ${this::class}")
  }

private fun String.toMessageFieldName(): MessageFieldName {
  check(last() == '_') { "Unsupported field $this in Message; must end with '_'" }
  return dropLast(1).toSnakeCase()
}
