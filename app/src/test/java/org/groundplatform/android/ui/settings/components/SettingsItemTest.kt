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
class SettingsItemTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `Displays title and summary`() {
    composeTestRule.setContent {
      AppTheme {
        SettingsItem(
          trailingIcon = R.drawable.ic_language,
          title = "Title",
          summary = "Summary",
          onClick = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Summary").assertIsDisplayed()
  }

  @Test
  fun `Should hide summary when null`() {
    composeTestRule.setContent {
      AppTheme {
        SettingsItem(
          trailingIcon = R.drawable.ic_language,
          title = "Title",
          summary = null,
          onClick = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Summary").assertDoesNotExist()
  }

  @Test
  fun `Invokes onClick action when clicked`() {
    var clicked = false

    composeTestRule.setContent {
      AppTheme {
        SettingsItem(
          trailingIcon = R.drawable.ic_language,
          title = "Title",
          summary = "Summary",
          onClick = { clicked = true },
        )
      }
    }

    composeTestRule.onNodeWithText("Title").performClick()
    assert(clicked)
  }
}
