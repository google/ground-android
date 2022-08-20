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
package com.google.android.ground.system.auth

import com.google.android.ground.model.User
import java8.util.Optional

data class SignInState(
    val state: State,
    val result: Result<User?>
) {

    enum class State {
        SIGNED_OUT, SIGNING_IN, SIGNED_IN, ERROR
    }

    fun user(): Optional<User> =
        if (result.isSuccess) Optional.ofNullable(result.getOrNull()) else Optional.empty()

    companion object {

        @JvmStatic
        fun signedOut() = SignInState(State.SIGNED_OUT, Result.success(null))

        @JvmStatic
        fun signingIn() = SignInState(State.SIGNING_IN, Result.success(null))

        @JvmStatic
        fun signedIn(user: User) = SignInState(State.SIGNED_IN, Result.success(user))

        @JvmStatic
        fun error(error: Throwable) = SignInState(State.ERROR, Result.failure(error))
    }
}
