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

/** The result of an operation that can return either true, false, or fail with an exception. */
data class BooleanOrError(val result: Result<Boolean>) {
  /**
   * Returns true if the operation succeeded with a result of `true`, or false otherwise. Note that
   * false is also returned if the operation failed in error.
   */
  val isTrue = value().orElse(false)!!

  fun value(): Optional<Boolean> = Optional.ofNullable(result.getOrNull())

  fun error(): Optional<Throwable?> = Optional.ofNullable(result.exceptionOrNull())

  companion object {
    @JvmStatic
    fun trueValue(): BooleanOrError {
      return BooleanOrError(Result.success(true))
    }

    @JvmStatic
    fun falseValue(): BooleanOrError {
      return BooleanOrError(Result.success(false))
    }

    @JvmStatic
    fun error(t: Throwable): BooleanOrError {
      return BooleanOrError(Result.failure(t))
    }
  }
}
