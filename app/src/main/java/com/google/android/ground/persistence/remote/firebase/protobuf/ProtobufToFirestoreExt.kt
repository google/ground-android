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
import timber.log.Timber

/**
 * Returns the map representation of this message ready for serialization in Firestore. The map is
 * keyed by message field numbers represented as strings, while field values are converted to
 * corresponding Firestore data types. Caveats:
 * * Proto field names starting with `bit_field` are not supported.
 * * If specified, the field with the provided field number will be skipped. This is useful when
 *   converting documents, in which case IDs are specified externally on they write call.
 */
fun Message.toFirestoreMap(idField: MessageFieldNumber? = null): FirestoreMap =
  this::class
    .getFieldProperties()
    .filter { hasValue(it) }
    .mapNotNull { toFirestoreMapEntry(it, idField) }
    .toMap()

private fun Message.toFirestoreMapEntry(
  property: KProperty<*>,
  idField: MessageFieldNumber?,
): Pair<FirestoreKey, FirestoreValue>? =
  try {
    val fieldName = property.name.toMessageFieldName()
    val fieldNumber =
      this::class.getFieldNumber(fieldName)
        ?: getSetOneOfFieldNumber(fieldName)
        ?: throw UnsupportedOperationException(
          "Unsupported protobuf-lite property $fieldName in $javaClass"
        )
    if (idField == fieldNumber) null else toFirestoreMapEntryUnchecked(fieldNumber, property)
  } catch (e: Throwable) {
    Timber.v(e, "Skipping property $property")
    null
  }

private fun Message.toFirestoreMapEntryUnchecked(
  fieldNumber: MessageFieldNumber,
  property: KProperty<*>,
): Pair<FirestoreKey, FirestoreValue>? {
  val key = fieldNumber.toString()
  val value = get(property)?.toFirestoreValue()
  return value?.let { key to value }
}

private fun Message.hasValue(property: KProperty<*>): Boolean {
  val value = get(property)
  return value != null && value != defaultInstanceForType.get(property)
}

private fun MessageValue.toFirestoreValue(): FirestoreValue =
  // TODO: Convert enums and other types.
  // Issue URL: https://github.com/google/ground-android/issues/1748
  when (this) {
    is List<*> -> map { it?.toFirestoreValue() }
    is Message -> toFirestoreMap()
    is Map<*, *> -> mapValues { it.value?.toFirestoreValue() }
    is Boolean,
    is String,
    is Number -> this
    else -> throw UnsupportedOperationException("Unsupported type ${this::class}")
  }

private fun String.toMessageFieldName(): MessageFieldName {
  check(last() == '_') { "Unsupported field $this in Message; must end with '_'" }
  return dropLast(1).toSnakeCase()
}
