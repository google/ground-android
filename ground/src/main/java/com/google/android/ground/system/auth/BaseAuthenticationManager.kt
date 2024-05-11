/*
 * Copyright 2024 Google LLC
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

abstract class BaseAuthenticationManager : AuthenticationManager {

  private var isInitialized: Boolean = false

  protected abstract fun initInternal()

  override fun init() {
    initInternal()
    isInitialized = true
  }

  override suspend fun getAuthenticatedUser(): User {
    if (!isInitialized) {
      init()
    }

    return signInState
      .filter { it.state == SignInState.State.SIGNED_IN }
      .mapNotNull { it.result.getOrNull() }
      .first()
  }
}
