/*
 * Copyright 2026 Google LLC
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

package org.groundplatform.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.auth.SignInState
import org.groundplatform.domain.system.auth.AuthenticationManager

/** In-memory test double for [AuthenticationManager]. */
class FakeAuthenticationManager(
  var authenticatedUser: User = User("user_id", "user@example.com", "Test User")
) : AuthenticationManager {
  private val _signInState = MutableStateFlow<SignInState>(SignInState.SignedIn(authenticatedUser))
  override val signInState: Flow<SignInState> = _signInState.asStateFlow()

  var signInCalled = false
  var signOutCalled = false
  var initCalled = false

  override fun init() {
    initCalled = true
  }

  override fun signIn() {
    signInCalled = true
    _signInState.value = SignInState.SignedIn(authenticatedUser)
  }

  override fun signOut() {
    signOutCalled = true
    _signInState.value = SignInState.SignedOut
  }

  override suspend fun getAuthenticatedUser(): User = authenticatedUser
}
