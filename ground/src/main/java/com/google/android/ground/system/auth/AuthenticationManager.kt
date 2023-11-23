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
import io.reactivex.Observable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlow

interface AuthenticationManager {
  val signInState: Observable<SignInState>

  fun init()

  fun signIn()

  fun signOut()

  /** Returns the logged-in user. */
  suspend fun getAuthenticatedUser(): User =
    signInState
      .asFlow()
      .filter { it.state == SignInState.State.SIGNED_IN }
      .map { it.result.getOrNull() }
      .filterNotNull()
      .first()
}
