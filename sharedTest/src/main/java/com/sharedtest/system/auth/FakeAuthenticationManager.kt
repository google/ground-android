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
package com.sharedtest.system.auth

import com.google.android.ground.model.User
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.system.auth.SignInState
import com.google.android.ground.system.auth.SignInState.Companion.signedIn
import com.google.android.ground.system.auth.SignInState.Companion.signedOut
import com.sharedtest.FakeData
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAuthenticationManager @Inject constructor() : AuthenticationManager {

  private val behaviourSubject: @Hot(replays = true) Subject<SignInState> = BehaviorSubject.create()

  // TODO: Remove default user once instrumentation tests can set it during the test. Currently, the
  // activity gets launched before the user can be set in setUp()
  override var currentUser: User = FakeData.USER
    private set

  override val signInState: Observable<SignInState>
    get() = behaviourSubject

  fun setUser(user: User) {
    currentUser = user
  }

  fun setState(state: SignInState) = behaviourSubject.onNext(state)

  override fun init() = behaviourSubject.onNext(signedIn(currentUser))

  override fun signIn() = behaviourSubject.onNext(signedIn(currentUser))

  override fun signOut() = behaviourSubject.onNext(signedOut())
}
