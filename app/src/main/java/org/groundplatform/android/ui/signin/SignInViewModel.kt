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
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.NetworkStatus
import org.groundplatform.android.system.auth.SignInState
import org.groundplatform.android.ui.common.AbstractViewModel

class SignInViewModel
@Inject
internal constructor(
  private val networkManager: NetworkManager,
  private val userRepository: UserRepository,
) : AbstractViewModel() {

  @OptIn(ExperimentalCoroutinesApi::class)
  fun getNetworkFlow(): Flow<Boolean> =
    networkManager.networkStatusFlow
      .mapLatest { it == NetworkStatus.AVAILABLE }
      .shareIn(viewModelScope, SharingStarted.Lazily, replay = 0)

  fun onSignInButtonClick() {
    viewModelScope.launch {
      val state = userRepository.getSignInState().first()
      if (state is SignInState.SignedOut || state is SignInState.Error) {
        userRepository.signIn()
      }
    }
  }
}
