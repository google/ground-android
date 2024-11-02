/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PermissionDeniedDialogTest : BaseHiltTest() {

  /**
   * composeTestRule has to be created in the specific test file in order to access the required
   * activity. [composeTestRule.activity]
   */
  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun permissionDeniedDialog_DisplaysCorrectTitle() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "http://example.com", onSignOut = {}, onCloseApp = {})
    }

    // Check if the title is displayed correctly
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.permission_denied))
      .assertIsDisplayed()
  }

  @Test
  fun permissionDeniedDialog_DisplaysSignOutButton() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "http://example.com", onSignOut = {}, onCloseApp = {})
    }

    // Check if the sign-out button is displayed
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out))
      .assertIsDisplayed()
  }

  @Test
  fun permissionDeniedDialog_DisplaysCloseAppButton() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "http://example.com", onSignOut = {}, onCloseApp = {})
    }

    // Check if the close app button is displayed
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.close_app))
      .assertIsDisplayed()
  }

  @Test
  fun permissionDeniedDialog_ClickSignOutButton_CallsOnSignOut() {
    var signOutCalled = false

    composeTestRule.setContent {
      PermissionDeniedDialog(
        signupLink = "http://example.com",
        onSignOut = { signOutCalled = true },
        onCloseApp = {},
      )
    }

    // Click the sign-out button
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out))
      .performClick()

    // Check if onSignOut was called
    assert(signOutCalled)
  }

  @Test
  fun permissionDeniedDialog_ClickCloseAppButton_CallsOnCloseApp() {
    var closeAppCalled = false

    composeTestRule.setContent {
      PermissionDeniedDialog(
        signupLink = "http://example.com",
        onSignOut = {},
        onCloseApp = { closeAppCalled = true },
      )
    }

    // Click the close app button
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.close_app))
      .performClick()

    // Check if onCloseApp was called
    assert(closeAppCalled)
  }

  @Test
  fun permissionDeniedDialog_DisplaysSignupLink_WhenLinkIsProvided() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "http://example.com", onSignOut = {}, onCloseApp = {})
    }

    // Check if the signup link is displayed
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.signup_request_access))
      .assertIsDisplayed()
  }

  @Test
  fun permissionDeniedDialog_DoesNotDisplaySignupLink_WhenLinkIsEmpty() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "", onSignOut = {}, onCloseApp = {})
    }

    // Check if the signup link is not displayed
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.signup_request_access))
      .assertDoesNotExist()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.admin_request_access))
      .assertIsDisplayed()
  }

  @Test
  fun signOutWarning_isDisplayed() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "", onSignOut = {}, onCloseApp = {})
    }
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.signout_warning))
      .assertIsDisplayed()
  }
}
