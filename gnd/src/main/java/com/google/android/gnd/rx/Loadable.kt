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
package com.google.android.gnd.rx

import androidx.lifecycle.LiveData
import com.google.android.gnd.persistence.remote.NotFoundException
import io.reactivex.Flowable
import java8.util.Optional
import org.reactivestreams.Publisher

/**
 * Wraps the state of an entity that can be loaded asynchronously. Based on `Resource` in
 * Android Guide to App Architecture: https://developer.android.com/jetpack/docs/guide#addendum
 *
 * @param <T> the type of data payload the resource contains.
</T> */
class Loadable<T> private constructor(val state: LoadState, data: T?, error: Throwable?) :
    ValueOrError<T>(data, error) {

    enum class LoadState {
        NOT_LOADED, LOADING, LOADED, NOT_FOUND, ERROR
    }

    val isLoaded = state == LoadState.LOADED

    companion object {
        @JvmStatic
        fun <T> notLoaded(): Loadable<T> = Loadable(LoadState.NOT_LOADED, null, null)

        fun <T> loading(): Loadable<T> = Loadable(LoadState.LOADING, null, null)

        @JvmStatic
        fun <T> loaded(data: T): Loadable<T> = Loadable(LoadState.LOADED, data, null)

        @JvmStatic
        fun <T> error(t: Throwable): Loadable<T> = Loadable(t.toState(), null, t)

        @JvmStatic
        fun <T> getValue(liveData: LiveData<Loadable<T?>?>): Optional<T?> =
            liveData.value?.value() ?: Optional.empty()

        /**
         * Modifies the provided stream to emit values instead of [Loadable] only when a value is
         * loaded (i.e., omitting intermediate loading and error states).
         */
        @JvmStatic
        fun <V> values(stream: Flowable<Loadable<V?>>): Publisher<V> =
            stream.filter { it.isPresent }.map { it.value().get() }

        /**
         * Returns a [Flowable] that first emits LOADING, then maps values emitted from the source
         * stream to `Loadable`s with a LOADED `Loadable`. Errors in the provided stream are
         * handled and wrapped in a `Loadable` with state ERROR. The returned stream itself should
         * never fail with an error.
         *
         * @param source the stream responsible for loading values.
         * @param <T> the type of entity being loaded
        </T> */
        @JvmStatic
        fun <T> loadingOnceAndWrap(source: Flowable<T>): Flowable<Loadable<T>> =
            source
                .map { data: T -> loaded(data) }
                .onErrorReturn { t: Throwable -> error(t) }
                .startWith(Loadable(LoadState.LOADING, null, null))
    }
}

private fun Throwable.toState(): Loadable.LoadState = when (this) {
    is NotFoundException -> Loadable.LoadState.NOT_FOUND
    else -> Loadable.LoadState.ERROR
}