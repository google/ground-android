/*
 * Copyright 2023 Google LLC
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

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.groundplatform.android.coroutines.ApplicationScope
import org.groundplatform.android.model.User

private val anonymousUser = User("nobody", "nobody", "Anonymous user ", null, true)

/**
 * Forces anonymous sign in with test user used when running against local Firebase Emulator Suite.
 */
class AnonymousAuthenticationManager
@Inject
constructor(
  private val firebaseAuth: FirebaseAuth,
  @ApplicationScope private val externalScope: CoroutineScope,
) : BaseAuthenticationManager() {
  private val _signInStateFlow = MutableStateFlow<SignInState?>(null)
  override val signInState: Flow<SignInState> = _signInStateFlow.asStateFlow().filterNotNull()

  override fun initInternal() {
    setState(
      if (firebaseAuth.currentUser == null) SignInState.SignedOut
      else SignInState.SignedIn(anonymousUser)
    )
  }

  private fun setState(nextState: SignInState) {
    _signInStateFlow.update { nextState }
  }

  override fun signIn() {
    setState(SignInState.SigningIn)
    externalScope.launch { firebaseAuth.signInAnonymously().await() }
    setState(SignInState.SignedIn(anonymousUser))
  }

  override fun signOut() {
    firebaseAuth.signOut()
    setState(SignInState.SignedOut)
  }
}
