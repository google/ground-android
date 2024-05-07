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
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

fun Message.toMap(): FirestoreMap =
  javaClass.kotlin.declaredMemberProperties.mapNotNull { toFirestoreValue(it) }.toMap()

private fun Message.toFirestoreValue(property: KProperty<*>): Pair<String, Any>? {
  val key = property.name.toFirestoreKey()
  val value = property.get(this)
  println("$key : $value")
  return null
}

private fun KProperty<*>.get(obj: Any): Any? {
  isAccessible = true
  val value = call(obj)
  isAccessible = false
  return value
}

private fun String.toFirestoreKey(): FirestoreKey {
  check(last() == '_') { "Unsupported field $this in Message; must end with '_'" }
  return dropLast(1)
}
