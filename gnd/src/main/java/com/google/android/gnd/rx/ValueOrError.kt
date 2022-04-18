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
package com.google.android.gnd.rx

import io.reactivex.Observable
import java8.util.Optional
import java8.util.function.Supplier

/**
 * Represents the outcome of an operation that either succeeds with a value, or fails with an
 * exception.
 *
 * @param <T> the type of value held by instances of this `ValueOrError`.
</T> */
open class ValueOrError<T> protected constructor(
    private val value: T?,
    private val error: Throwable?
) {
    val isPresent = value().isPresent

    fun value(): Optional<T> = Optional.ofNullable(value)

    fun error(): Optional<Throwable?> = Optional.ofNullable(error)

    companion object {
        /** Returns the value returned by the specified supplier, or an error if the supplier fails.  */
        @JvmStatic
        fun <T> create(supplier: Supplier<T>): ValueOrError<T?> =
            try {
                newValue(supplier.get())
            } catch (e: Throwable) {
                newError(e)
            }

        /** Returns a new instance with the specified value.  */
        private fun <T> newValue(value: T): ValueOrError<T?> = ValueOrError(value, null)

        /** Returns a new instance with the specified error.  */
        private fun <T> newError(t: Throwable?): ValueOrError<T?> = ValueOrError(null, t)

        /** Modifies the specified stream to ignore errors, returning wrapped values.  */
        @JvmStatic
        fun <T> ignoreErrors(observable: Observable<ValueOrError<T>>): Observable<T> =
            observable.filter { it.isPresent }.map { it.value().get() }

    }
}