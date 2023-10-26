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
package com.google.android.ground.system.auth

import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.model.User
import com.google.android.ground.rx.annotations.Hot
import com.google.firebase.auth.*
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.tasks.await

private val anonymousUser = User("nobody", "nobody", "Anonymous user ", null, true)

/**
 * Forces anonymous sign in with test user used when running against local Firebase Emulator Suite.
 */
class AnonymousAuthenticationManager
@Inject
constructor(
  private val firebaseAuth: FirebaseAuth,
  @ApplicationScope private val externalScope: CoroutineScope
) : AuthenticationManager {
  override val signInState: @Hot(replays = true) Subject<SignInState> = BehaviorSubject.create()

  /**
   * Returns the current user, blocking until a user logs in. Only call from code where user is
   * guaranteed to be authenticated.
   */
  override val currentUser: User
    get() =
      signInState
        .filter { it.state == SignInState.State.SIGNED_IN }
        .map { it.result.getOrNull()!! }
        .blockingFirst() // TODO: Should this be blocking?

  override fun init() {
    signInState.onNext(
      if (firebaseAuth.currentUser == null) SignInState.signedOut()
      else SignInState.signedIn(anonymousUser)
    )
  }

  override fun signIn() {
    signInState.onNext(SignInState.signingIn())
    externalScope.launch {
      firebaseAuth.signInAnonymously().await()
      signInState.onNext(SignInState.signedIn(anonymousUser))
    }
  }

  override fun signOut() {
    firebaseAuth.signOut()
    signInState.onNext(SignInState.signedOut())
  }
}
