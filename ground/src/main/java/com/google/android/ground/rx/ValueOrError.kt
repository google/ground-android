/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.rx

import java8.util.Optional

/**
 * Represents the outcome of an operation that either succeeds with a value, or fails with an
 * exception.
 *
 * @param <T> the type of value held by instances of this `ValueOrError`. </T>
 */
open class ValueOrError<T>
protected constructor(private val value: T?, private val error: Throwable?) {
  fun value(): Optional<T> = Optional.ofNullable(value)

  fun error(): Optional<Throwable?> = Optional.ofNullable(error)
}
