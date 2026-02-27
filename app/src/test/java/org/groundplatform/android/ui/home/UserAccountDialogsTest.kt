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
package org.groundplatform.android.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import org.groundplatform.android.FakeData
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.android.model.User
import org.groundplatform.android.ui.home.HomeScreenViewModel.AccountDialogState
import org.groundplatform.android.ui.home.HomeScreenViewModel.AccountDialogState.USER_DETAILS
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserAccountDialogsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var user: User

  @Before
  fun setUp() {
    user = FakeData.USER
  }

  private fun setupContent(
    state: AccountDialogState,
    onSignOut: () -> Unit = {},
    onShowSignOutConfirmation: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      UserAccountDialogs(
        state = state,
        user = user,
        onSignOut = onSignOut,
        onShowSignOutConfirmation = onShowSignOutConfirmation,
        onDismiss = {},
      )
    }
  }

  @Test
  fun showUserDetailsDialog() {
    setupContent(state = USER_DETAILS)

    composeTestRule.onNodeWithText(user.displayName).assertExists()
    composeTestRule.onNodeWithText(user.email).assertExists()
  }

  @Test
  fun showSignOutConfirmationDialog() {
    setupContent(state = AccountDialogState.SIGN_OUT_CONFIRMATION)

    composeTestRule.onNodeWithText(getString(R.string.sign_out_dialog_title)).assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.sign_out_dialog_body)).assertIsDisplayed()
  }

  @Test
  fun clickSignOut_invokesCallback() {
    var signOutClicked = false
    setupContent(
      state = AccountDialogState.SIGN_OUT_CONFIRMATION,
      onSignOut = { signOutClicked = true },
    )

    composeTestRule.onNodeWithText(getString(R.string.sign_out)).performClick()

    assertThat(signOutClicked, `is`(true))
  }

  @Test
  fun clickSignOutInUserDetails_invokesShowSignOutConfirmation() {
    var showSignOutConfirmationClicked = false
    setupContent(state = USER_DETAILS, onShowSignOutConfirmation = { showSignOutConfirmationClicked = true })

    composeTestRule.onNodeWithText(getString(R.string.sign_out)).performClick()

    assertThat(showSignOutConfirmationClicked, `is`(true))
  }
}
