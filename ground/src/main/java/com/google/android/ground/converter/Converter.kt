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
package com.google.android.ground.converter

/** Defines methods for converting classes to/from other representations. */
sealed interface Converter<T, R> {
  /** Converts a T object to its R representation. */
  fun convertTo(model: T): R?
  /** Converts an R object to its T representation. */
  fun convertFrom(entity: R): T?
}
