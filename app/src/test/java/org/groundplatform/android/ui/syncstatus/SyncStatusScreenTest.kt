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

package org.groundplatform.android.ui.syncstatus

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncStatusScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `Toolbar title is displayed`() {
    composeTestRule.setContent {
      AppTheme { SyncStatusScreen(uiState = SyncStatusState(), onNavigateUp = {}) }
    }

    composeTestRule.onNodeWithText("Data sync status").assertIsDisplayed()
  }

  @Test
  fun `Navigate up click triggers callback`() {
    var navigatedUp = false
    composeTestRule.setContent {
      AppTheme {
        SyncStatusScreen(
          uiState = SyncStatusState(),
          onNavigateUp = { navigatedUp = true },
        )
      }
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assertThat(navigatedUp).isTrue()
  }

  @Test
  fun `Empty state displays list container`() {
    composeTestRule.setContent {
      AppTheme { SyncStatusScreen(uiState = SyncStatusState(), onNavigateUp = {}) }
    }

    composeTestRule.onNodeWithTag(SYNC_STATUS_LIST_TEST_TAG).assertIsDisplayed()
  }

  @Test
  fun `Loaded state displays list and items`() {
    val detail =
      SyncStatusDetail(
        user = "Jane Doe",
        status = Mutation.SyncStatus.PENDING,
        timestamp = 1700000000000L,
        label = "Map the farms",
        subtitle = "IDX21311",
        description = "Lacuna Fund Cocoa Mapping",
      )

    composeTestRule.setContent {
      AppTheme {
        SyncStatusScreen(
          uiState = SyncStatusState(items = listOf(detail)),
          onNavigateUp = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(SYNC_STATUS_LIST_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Map the farms • IDX21311").assertIsDisplayed()
    composeTestRule.onNodeWithText("Lacuna Fund Cocoa Mapping").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pending").assertIsDisplayed()
  }

  @Test
  fun `Custom modifier is applied to root`() {
    composeTestRule.setContent {
      AppTheme {
        SyncStatusScreen(
          uiState = SyncStatusState(),
          onNavigateUp = {},
          modifier = Modifier.testTag("root_test_tag"),
        )
      }
    }

    composeTestRule.onNodeWithTag("root_test_tag").assertIsDisplayed()
  }
}
