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
package com.google.android.ground.rx

import java8.util.Optional

/**
 * Wraps the state of an entity that can be loaded asynchronously. Based on `Resource` in Android
 * Guide to App Architecture: https://developer.android.com/jetpack/docs/guide#addendum
 *
 * @param <T> the type of data payload the resource contains. </T>
 */
data class Loadable<T>(val state: LoadState, val result: Result<T?>) {

  enum class LoadState {
    LOADED,
    ERROR
  }

  // TODO: Remove these once all dependencies are migrated to Kotlin
  fun value(): Optional<T> = Optional.ofNullable(result.getOrNull())

  val isLoaded = state == LoadState.LOADED

  companion object {
    @JvmStatic
    fun <T> loaded(data: T): Loadable<T> = Loadable(LoadState.LOADED, Result.success(data))

    @JvmStatic
    fun <T> error(t: Throwable): Loadable<T> = Loadable(LoadState.ERROR, Result.failure(t))
  }
}
