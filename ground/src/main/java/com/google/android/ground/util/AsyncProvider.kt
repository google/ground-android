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

open class AsyncProvider<T>(private val providerFunction: suspend () -> T) {
  private var instance: T? = null
  private val mutex = Mutex()

  suspend fun get(): T =
    mutex.withLock {
      if (instance == null) {
        instance = providerFunction()
      }
      return instance!!
    }
}
