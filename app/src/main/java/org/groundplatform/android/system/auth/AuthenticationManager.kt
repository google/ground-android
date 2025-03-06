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
package org.groundplatform.android.system.auth

import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.model.User

interface AuthenticationManager {
  val signInState: Flow<SignInState>

  /** Must be called before looking up auth state or logged-in user. */
  fun init()

  fun signIn()

  fun signOut()

  /** Returns the logged-in user. */
  suspend fun getAuthenticatedUser(): User
}
