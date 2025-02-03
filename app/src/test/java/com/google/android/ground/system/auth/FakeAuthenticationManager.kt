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

import com.google.android.ground.FakeData
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Singleton
class FakeAuthenticationManager
@Inject
constructor(@ApplicationScope private val externalScope: CoroutineScope) :
  BaseAuthenticationManager() {

  private val _signInStateFlow = MutableStateFlow<SignInState?>(null)
  override val signInState: Flow<SignInState> = _signInStateFlow.asStateFlow().filterNotNull()

  private var currentUser: User = FakeData.USER

  fun setUser(user: User) {
    currentUser = user
  }

  fun setState(state: SignInState) {
    externalScope.launch { _signInStateFlow.emit(state) }
  }

  override fun initInternal() = setState(SignInState.SignedIn(currentUser))

  override fun signIn() = setState(SignInState.SignedIn(currentUser))

  override fun signOut() = setState(SignInState.SignedOut)
}
