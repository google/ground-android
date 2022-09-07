/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.util

import java8.util.function.Supplier
import java8.util.stream.Stream
import java8.util.stream.StreamSupport
import timber.log.Timber

/** Methods for working with and manipulating [java8.util.stream.Stream]. */
object StreamUtil {
  /**
   * Executes the specified Supplier, writing throw Error exceptions to debug logs. If an error
   * occurs, an empty Stream is returned, otherwise a Stream with only the result of the Supplier is
   * returned.
   */
  @JvmStatic
  fun <R> logErrorsAndSkip(supplier: Supplier<R>): Stream<R> {
    return try {
      StreamSupport.stream(setOf(supplier.get()))
    } catch (e: RuntimeException) {
      Timber.e(e)
      StreamSupport.stream(emptySet())
    }
  }

  fun <R> logErrorsAndSkipKt(supplier: Supplier<R>): Iterable<R> {
    return try {
      setOf(supplier.get())
    } catch (e: RuntimeException) {
      Timber.e(e)
      emptySet()
    }
  }
}
