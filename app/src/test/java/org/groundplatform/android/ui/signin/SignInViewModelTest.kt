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
package org.groundplatform.android.ui.signin

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.model.User
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.NetworkStatus
import org.groundplatform.android.system.auth.SignInState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class SignInViewModelTest {

  @Mock private lateinit var networkManager: NetworkManager
  @Mock private lateinit var userRepository: UserRepository

  private lateinit var viewModel: SignInViewModel

  private val signInStateFlow = MutableStateFlow<SignInState>(SignInState.SignedOut)
  private val networkStatusFlow = MutableStateFlow(NetworkStatus.UNAVAILABLE)

  @Before
  fun setUp() {
    `when`(userRepository.getSignInState()).thenReturn(signInStateFlow)
    `when`(networkManager.networkStatusFlow).thenReturn(networkStatusFlow)
    `when`(networkManager.isNetworkConnected()).thenReturn(false)

    viewModel = SignInViewModel(networkManager, userRepository)
  }

  @Test
  fun `signInState reflects the value from userRepository`() = runTest {
    viewModel.signInState.test {
      assertThat(awaitItem()).isEqualTo(SignInState.SignedOut)

      signInStateFlow.value = SignInState.SigningIn
      assertThat(awaitItem()).isEqualTo(SignInState.SigningIn)

      val user = User("id", "email", "name")
      signInStateFlow.value = SignInState.SignedIn(user)
      assertThat(awaitItem()).isEqualTo(SignInState.SignedIn(user))
    }
  }

  @Test
  fun `networkAvailable reflects the value from networkManager`() = runTest {
    viewModel.networkAvailable.test {
      assertThat(awaitItem()).isFalse()

      networkStatusFlow.value = NetworkStatus.AVAILABLE
      assertThat(awaitItem()).isTrue()

      networkStatusFlow.value = NetworkStatus.UNAVAILABLE
      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `onSignInButtonClick calls signIn`() {
    viewModel.onSignInButtonClick()
    verify(userRepository).signIn()
  }

  @Test
  fun `onSignOutButtonClick calls signOut`() {
    viewModel.onSignOutButtonClick()
    verify(userRepository).signOut()
  }
}
