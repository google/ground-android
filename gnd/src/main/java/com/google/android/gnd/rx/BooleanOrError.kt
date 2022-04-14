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

/** The result of an operation that can return either true, false, or fail with an exception.  */
class BooleanOrError private constructor(value: Boolean?, error: Throwable?) :
    ValueOrError<Boolean?>(value, error) {
    /**
     * Returns true if the operation succeeded with a result of `true`, or false otherwise. Note
     * that false is also returned if the operation failed in error.
     */
    val isTrue = value().orElse(false)!!

    companion object {
        @JvmStatic
        fun trueValue(): BooleanOrError {
            return BooleanOrError(true, null)
        }

        @JvmStatic
        fun falseValue(): BooleanOrError {
            return BooleanOrError(false, null)
        }

        @JvmStatic
        fun error(t: Throwable?): BooleanOrError {
            return BooleanOrError(null, t)
        }
    }
}