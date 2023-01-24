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

import androidx.lifecycle.LiveData
import io.reactivex.Flowable
import java8.util.Optional
import org.reactivestreams.Publisher

/**
 * Wraps the state of an entity that can be loaded asynchronously. Based on `Resource` in Android
 * Guide to App Architecture: https://developer.android.com/jetpack/docs/guide#addendum
 *
 * @param <T> the type of data payload the resource contains. </T>
 */
data class Loadable<T> private constructor(val state: LoadState, val result: Result<T?>) {

  enum class LoadState {
    NOT_LOADED,
    LOADING,
    LOADED,
    ERROR
  }

  // TODO: Remove these once all dependencies are migrated to Kotlin
  fun value(): Optional<T> = Optional.ofNullable(result.getOrNull())

  // TODO: Remove these once all dependencies are migrated to Kotlin
  fun error(): Optional<Throwable> = Optional.ofNullable(result.exceptionOrNull())

  val isLoaded = state == LoadState.LOADED

  companion object {
    fun <T> notLoaded(): Loadable<T> = Loadable(LoadState.NOT_LOADED, Result.success(null))

    fun <T> loading(): Loadable<T> = Loadable(LoadState.LOADING, Result.success(null))

    @JvmStatic
    fun <T> loaded(data: T): Loadable<T> = Loadable(LoadState.LOADED, Result.success(data))

    @JvmStatic
    fun <T> error(t: Throwable): Loadable<T> = Loadable(LoadState.ERROR, Result.failure(t))

    @JvmStatic
    fun <T> getValue(liveData: LiveData<Loadable<T?>?>): Optional<T?> =
      liveData.value?.value() ?: Optional.empty()

    /**
     * Modifies the provided stream to emit values instead of [Loadable] only when a value is loaded
     * (i.e., omitting intermediate loading and error states).
     */
    @JvmStatic
    fun <V> values(stream: Flowable<Loadable<V>>): Publisher<V> =
      stream.filter { it.isLoaded }.map { it.value().get() }

    /**
     * Returns a [Flowable] that first emits LOADING, then maps values emitted from the source
     * stream to `Loadable`s with a LOADED `Loadable`. Errors in the provided stream are handled and
     * wrapped in a `Loadable` with state ERROR. The returned stream itself should never fail with
     * an error.
     *
     * @param source the stream responsible for loading values.
     * @param <T> the type of entity being loaded </T>
     */
    fun <T> loadingOnceAndWrap(source: Flowable<T>): Flowable<Loadable<T>> =
      source
        .map { data: T -> loaded(data) }
        .onErrorReturn { t: Throwable -> error(t) }
        .startWith(loading())
  }
}
