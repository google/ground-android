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
package com.google.android.gnd.system.auth

import com.google.android.gnd.model.User
import com.google.android.gnd.rx.ValueOrError

class SignInState : ValueOrError<User> {
    val state: State
    val user = value()

    enum class State {
        SIGNED_OUT, SIGNING_IN, SIGNED_IN, ERROR
    }

    constructor(state: State) : super(null, null) {
        this.state = state
    }

    constructor(user: User) : super(user, null) {
        state = State.SIGNED_IN
    }

    constructor(error: Throwable) : super(null, error) {
        state = State.ERROR
    }
}
