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

package org.groundplatform.android.ui.offlineareas

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.groundplatform.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineAreasScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `Toolbar title is displayed`() {
    composeTestRule.setContent {
      AppTheme {
        OfflineAreasScreen(
          uiState = OfflineAreasState(isLoading = false),
          onAreaClick = {},
          onSelectAreaClick = {},
          onNavigateUp = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Offline map imagery").assertIsDisplayed()
  }

  @Test
  fun `Navigate up click triggers callback`() {
    var navigatedUp = false
    composeTestRule.setContent {
      AppTheme {
        OfflineAreasScreen(
          uiState = OfflineAreasState(isLoading = false),
          onAreaClick = {},
          onSelectAreaClick = {},
          onNavigateUp = { navigatedUp = true },
        )
      }
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assertThat(navigatedUp).isTrue()
  }

  @Test
  fun `Loading state shows progress spinner`() {
    composeTestRule.setContent {
      AppTheme {
        OfflineAreasScreen(
          uiState = OfflineAreasState(isLoading = true),
          onAreaClick = {},
          onSelectAreaClick = {},
          onNavigateUp = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(OFFLINE_AREAS_LOADING_SPINNER_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OFFLINE_AREAS_LIST_TEST_TAG).assertDoesNotExist()
  }

  @Test
  fun `Empty state shows illustration and no areas message`() {
    composeTestRule.setContent {
      AppTheme {
        OfflineAreasScreen(
          uiState = OfflineAreasState(offlineAreas = emptyList(), isLoading = false),
          onAreaClick = {},
          onSelectAreaClick = {},
          onNavigateUp = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(OFFLINE_AREAS_NO_AREAS_MESSAGE_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithText("No map imagery downloaded for offline use").assertIsDisplayed()
    composeTestRule.onNodeWithTag(OFFLINE_AREAS_LIST_TEST_TAG).assertDoesNotExist()
  }

  @Test
  fun `Loaded state shows header and offline areas list`() {
    val areas =
      listOf(
        OfflineAreaDetails("id_1", "Downtown", "1.2 MB"),
        OfflineAreaDetails("id_2", "Park", "3.4 MB"),
      )

    composeTestRule.setContent {
      AppTheme {
        OfflineAreasScreen(
          uiState = OfflineAreasState(offlineAreas = areas, isLoading = false),
          onAreaClick = {},
          onSelectAreaClick = {},
          onNavigateUp = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(OFFLINE_AREAS_LIST_TITLE_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Downloaded areas").assertIsDisplayed()
    composeTestRule.onNodeWithTag(OFFLINE_AREAS_LIST_TIP_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Available for offline viewing").assertIsDisplayed()

    composeTestRule.onNodeWithTag(OFFLINE_AREAS_LIST_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OFFLINE_AREAS_LIST_TEST_TAG).onChildren().assertCountEquals(2)
    composeTestRule.onNodeWithText("Downtown").assertIsDisplayed()
    composeTestRule.onNodeWithText("Park").assertIsDisplayed()
  }

  @Test
  fun `Clicking list item triggers onAreaClick`() {
    var clickedAreaId: String? = null
    val areas = listOf(OfflineAreaDetails("id_1", "Downtown", "1.2 MB"))

    composeTestRule.setContent {
      AppTheme {
        OfflineAreasScreen(
          uiState = OfflineAreasState(offlineAreas = areas, isLoading = false),
          onAreaClick = { clickedAreaId = it },
          onSelectAreaClick = {},
          onNavigateUp = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Downtown").performClick()
    assertThat(clickedAreaId).isEqualTo("id_1")
  }

  @Test
  fun `Clicking FAB triggers onSelectAreaClick`() {
    var fabClicked = false
    composeTestRule.setContent {
      AppTheme {
        OfflineAreasScreen(
          uiState = OfflineAreasState(isLoading = false),
          onAreaClick = {},
          onSelectAreaClick = { fabClicked = true },
          onNavigateUp = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(OFFLINE_AREAS_SELECT_FAB_TEST_TAG).performClick()
    assertThat(fabClicked).isTrue()
  }
}
