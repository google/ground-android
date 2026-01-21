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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.system.auth.SignInState
import org.groundplatform.android.ui.theme.AppTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SignInScreenTest : BaseHiltTest() {

  @Mock private lateinit var viewModel: SignInViewModel

  private val networkAvailableState: MutableStateFlow<Boolean> = MutableStateFlow(true)
  private val signInState: MutableStateFlow<SignInState> = MutableStateFlow(SignInState.SignedOut)

  override fun setUp() {
    super.setUp()
    whenever(viewModel.networkAvailable).thenReturn(networkAvailableState)
    whenever(viewModel.signInState).thenReturn(signInState)
    composeTestRule.setContent { AppTheme { SignInScreen(viewModel = viewModel) } }
  }

  @Test
  fun `Google sign-in button is displayed and enabled`() {
    composeTestRule.onNodeWithTag(BUTTON_TEST_TAG).assertIsDisplayed().assertIsEnabled()
  }

  @Test
  fun `Clicking Google sign-in button should invoke sign-in`() = runWithTestDispatcher {
    composeTestRule.onNodeWithTag(BUTTON_TEST_TAG).performClick()
    composeTestRule.waitForIdle()
    verify(viewModel).onSignInButtonClick()
  }

  @Test
  fun `Google sign-in button is disabled when not connected to the internet`() =
    runWithTestDispatcher {
      networkAvailableState.emit(false)
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(BUTTON_TEST_TAG).assertIsNotEnabled()
    }

  @Test
  fun `Google sign-in button is disabled when signing in`() = runWithTestDispatcher {
    signInState.emit(SignInState.SigningIn)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(BUTTON_TEST_TAG).assertIsNotEnabled()
  }

  @Test
  fun `Loading dialog is displayed when signing in`() = runWithTestDispatcher {
    signInState.emit(SignInState.SigningIn)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Signing in").assertIsDisplayed()
  }

  @Test
  fun `Error is displayed when not connected to the internet`() = runWithTestDispatcher {
    networkAvailableState.emit(false)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Connect to the internet to sign in").assertIsDisplayed()
  }
}
