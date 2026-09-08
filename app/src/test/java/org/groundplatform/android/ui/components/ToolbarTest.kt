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

package org.groundplatform.android.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToolbarTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `Toolbar displays title and back button by default`() {
    var iconClicked = false
    composeTestRule.setContent {
      Toolbar(stringRes = R.string.offline_map_imagery, iconClick = { iconClicked = true })
    }

    composeTestRule.onNodeWithText("Offline map imagery").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
    assertThat(iconClicked).isTrue()
  }

  @Test
  fun `Toolbar with showNavigationIcon false hides back button`() {
    composeTestRule.setContent {
      Toolbar(
        stringRes = R.string.offline_map_imagery,
        showNavigationIcon = false,
        iconClick = {},
      )
    }

    composeTestRule.onNodeWithText("Offline map imagery").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Back").assertDoesNotExist()
  }

  @Test
  fun `Toolbar with titleCentered true displays title and back button`() {
    var iconClicked = false
    composeTestRule.setContent {
      Toolbar(
        stringRes = R.string.offline_map_imagery,
        showNavigationIcon = true,
        titleCentered = true,
        iconClick = { iconClicked = true },
      )
    }

    composeTestRule.onNodeWithText("Offline map imagery").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
    assertThat(iconClicked).isTrue()
  }
}
