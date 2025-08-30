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
package org.groundplatform.android.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.Test
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PermissionDeniedDialogTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `PermissionDeniedDialog displays correct title`() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "http://example.com", onSignOut = {}, onCloseApp = {})
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.permission_denied))
      .assertIsDisplayed()
  }

  @Test
  fun `PermissionDeniedDialog displays sign out button`() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "http://example.com", onSignOut = {}, onCloseApp = {})
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out))
      .assertIsDisplayed()
  }

  @Test
  fun `PermissionDeniedDialog displays close app button`() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "http://example.com", onSignOut = {}, onCloseApp = {})
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.close_app))
      .assertIsDisplayed()
  }

  @Test
  fun `PermissionDeniedDialog calls onSignOut when sign out button is clicked`() {
    var signOutCalled = false

    composeTestRule.setContent {
      PermissionDeniedDialog(
        signupLink = "http://example.com",
        onSignOut = { signOutCalled = true },
        onCloseApp = {},
      )
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.sign_out))
      .performClick()

    assert(signOutCalled)
  }

  @Test
  fun `PermissionDeniedDialog calls onCloseApp when close app button is clicked`() {
    var closeAppCalled = false

    composeTestRule.setContent {
      PermissionDeniedDialog(
        signupLink = "http://example.com",
        onSignOut = {},
        onCloseApp = { closeAppCalled = true },
      )
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.close_app))
      .performClick()

    assert(closeAppCalled)
  }

  @Test
  fun `PermissionDeniedDialog displays signup link when link is provided`() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "http://example.com", onSignOut = {}, onCloseApp = {})
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.signup_request_access))
      .assertIsDisplayed()
  }

  @Test
  fun `PermissionDeniedDialog does not display signup link when link is empty`() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "", onSignOut = {}, onCloseApp = {})
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.signup_request_access))
      .assertDoesNotExist()
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.admin_request_access))
      .assertIsDisplayed()
  }

  @Test
  fun `SignOutWarning is displayed`() {
    composeTestRule.setContent {
      PermissionDeniedDialog(signupLink = "", onSignOut = {}, onCloseApp = {})
    }
    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.signout_warning))
      .assertIsDisplayed()
  }
}
