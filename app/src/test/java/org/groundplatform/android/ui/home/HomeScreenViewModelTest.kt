/*
 * Copyright 2025 Google LLC
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

package org.groundplatform.android.ui.home

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.system.auth.FakeAuthenticationManager
import org.groundplatform.android.system.auth.SignInState
import org.groundplatform.android.ui.home.HomeScreenViewModel.AccountDialogState.HIDDEN
import org.groundplatform.android.ui.home.HomeScreenViewModel.AccountDialogState.SIGN_OUT_CONFIRMATION
import org.groundplatform.android.ui.home.HomeScreenViewModel.AccountDialogState.USER_DETAILS
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenViewModelTest : BaseHiltTest() {

  @Inject lateinit var authenticationManager: FakeAuthenticationManager
  @Inject lateinit var viewModel: HomeScreenViewModel

  @Before
  override fun setUp() {
    super.setUp()
    authenticationManager.setUser(FakeData.USER)
    authenticationManager.signIn()
  }

  @Test
  fun testShowUserDetails() {
    viewModel.showUserDetails()

    assertThat(viewModel.accountDialogState.value).isEqualTo(USER_DETAILS)
  }

  @Test
  fun testShowSignOutConfirmation() {
    viewModel.showSignOutConfirmation()

    assertThat(viewModel.accountDialogState.value).isEqualTo(SIGN_OUT_CONFIRMATION)
  }

  @Test
  fun testDismissLogoutDialog() {
    viewModel.showUserDetails()
    viewModel.dismissLogoutDialog()

    assertThat(viewModel.accountDialogState.value).isEqualTo(HIDDEN)
  }

  @Test
  fun testSignOut() = runWithTestDispatcher {
    viewModel.showSignOutConfirmation()
    viewModel.signOut()

    advanceUntilIdle()

    assertThat(viewModel.accountDialogState.value).isEqualTo(HIDDEN)
    assertThat(authenticationManager.signInState.filterIsInstance<SignInState.SignedOut>().first())
      .isEqualTo(SignInState.SignedOut)
  }

  @Test
  fun testAuthenticatedUser() = runWithTestDispatcher {
    advanceUntilIdle()

    val user = viewModel.user.filterNotNull().first()
    assertThat(user).isEqualTo(FakeData.USER)
  }
}
