/*
 * Copyright 2018 Google LLC
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
package org.groundplatform.android.ui.signin

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.NetworkStatus
import org.groundplatform.android.system.auth.SignInState
import org.groundplatform.android.ui.common.AbstractViewModel

/** View model responsible for handling the sign-in screen. */
@HiltViewModel
class SignInViewModel
@Inject
internal constructor(networkManager: NetworkManager, private val userRepository: UserRepository) :
  AbstractViewModel() {

  val signInState: StateFlow<SignInState> =
    userRepository
      .getSignInState()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = SignInState.SignedOut,
      )

  val networkAvailable: StateFlow<Boolean> =
    networkManager.networkStatusFlow
      .map { it == NetworkStatus.AVAILABLE }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = networkManager.isNetworkConnected(),
      )

  fun onSignInButtonClick() {
    if (signInState.value.shouldAllowSignIn() && networkAvailable.value) {
      viewModelScope.launch { userRepository.signIn() }
    }
  }
}
