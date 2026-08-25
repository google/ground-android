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
package org.groundplatform.android.ui.settings.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.groundplatform.android.R
import org.groundplatform.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsSwitchItemTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `Displays title and summary`() {
    composeTestRule.setContent {
      AppTheme {
        SettingsSwitchItem(
          icon = R.drawable.ic_cloud_upload,
          title = "Title",
          summary = "Summary",
          checked = true,
          onCheckedChange = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Summary").assertIsDisplayed()
  }

  @Test
  fun `Hides summary when null`() {
    composeTestRule.setContent {
      AppTheme {
        SettingsSwitchItem(
          icon = R.drawable.ic_cloud_upload,
          title = "Title",
          summary = null,
          checked = false,
          onCheckedChange = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Summary").assertDoesNotExist()
  }

  @Test
  fun `Switch should be on when checked state is true`() {
    composeTestRule.setContent {
      AppTheme {
        SettingsSwitchItem(
          icon = R.drawable.ic_cloud_upload,
          title = "Title",
          checked = true,
          onCheckedChange = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Title").assertIsOn()
  }

  @Test
  fun `Switch should be off when checked state is false`() {
    composeTestRule.setContent {
      AppTheme {
        SettingsSwitchItem(
          icon = R.drawable.ic_cloud_upload,
          title = "Title",
          checked = false,
          onCheckedChange = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Title").assertIsOff()
  }

  @Test
  fun `Invokes onCheckedChange with the correct value when clicked on`() {
    var newValue: Boolean? = null

    composeTestRule.setContent {
      AppTheme {
        SettingsSwitchItem(
          icon = R.drawable.ic_cloud_upload,
          title = "Title",
          checked = false,
          onCheckedChange = { newValue = it },
        )
      }
    }

    composeTestRule.onNodeWithText("Title").performClick()
    assert(newValue == true)
  }
}
