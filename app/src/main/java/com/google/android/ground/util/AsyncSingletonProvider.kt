/*
 * Copyright 2023 Google LLC
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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Base class for providers for singletons which need to be initialized asynchronously. */
open class AsyncSingletonProvider<T>(private val providerFunction: suspend () -> T) {
  @Volatile private var instance: T? = null
  private val mutex = Mutex()

  /**
   * Returns the singleton instance of the provided class, calling the [providerFunction] on the
   * first call to construct and initialize the object. Thread-safe; concurrent callers will block
   * until the singleton is created and returned.
   */
  suspend fun get(): T =
    instance ?: mutex.withLock { instance ?: providerFunction().also { instance = it } }
}
