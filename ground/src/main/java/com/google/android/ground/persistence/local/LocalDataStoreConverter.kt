/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.persistence.local

/** Defines methods for converting classes to/from other representations. */
interface LocalDataStoreConverter<T, R> {
  /** Converts a model (T) object to its data store (R) representation. */
  fun convertToDataStoreObject(model: T): R?
  /** Converts an data store (R) object to its model (T) representation. */
  fun convertFromDataStoreObject(entity: R): T?
}
