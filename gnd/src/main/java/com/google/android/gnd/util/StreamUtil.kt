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

package com.google.android.gnd.util;

import static java8.util.stream.StreamSupport.stream;

import java.util.Collections;
import java8.util.function.Supplier;
import java8.util.stream.Stream;
import timber.log.Timber;

/** Methods for working with and manipulating {@link java8.util.stream.Stream}s. */
public class StreamUtil {

  /**
   * Executes the specified Supplier, writing throw Error exceptions to debug logs. If an error
   * occurs, an empty Stream is returned, otherwise a Stream with only the result of the Supplier is
   * returned.
   */
  public static <R> Stream<R> logErrorsAndSkip(Supplier<R> supplier) {
    try {
      return stream(Collections.singleton(supplier.get()));
    } catch (RuntimeException e) {
      Timber.e(e);
      return stream(Collections.emptySet());
    }
  }
}
