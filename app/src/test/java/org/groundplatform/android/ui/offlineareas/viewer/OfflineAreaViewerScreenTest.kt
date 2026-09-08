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

package org.groundplatform.android.ui.offlineareas.viewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.assertTrue
import org.groundplatform.android.FakeData.OFFLINE_AREA
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineAreaViewerScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `displays toolbar title and back button`() {
    setContent()

    composeTestRule
      .onNodeWithText(getString(R.string.offline_area_viewer_title))
      .assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed().assertIsEnabled()
  }

  @Test
  fun `displays area name and size on disk when loaded`() {
    setContent(
      uiState =
        OfflineAreaViewerState(
          area = OFFLINE_AREA,
          areaName = OFFLINE_AREA.name,
          areaSize = "2.5",
          isProgressOverlayVisible = false,
        )
    )

    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_NAME_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals(OFFLINE_AREA.name)

    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_SIZE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals(getString(R.string.offline_area_size_on_disk_mb, "2.5"))
  }

  @Test
  fun `displays remove button as enabled when area is loaded`() {
    setContent(
      uiState =
        OfflineAreaViewerState(
          area = OFFLINE_AREA,
          areaName = OFFLINE_AREA.name,
          areaSize = "2.5",
          isProgressOverlayVisible = false,
        )
    )

    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG)
      .assertIsDisplayed()
      .assertIsEnabled()
      .assertTextContains(getString(R.string.offline_area_viewer_remove_button))
  }

  @Test
  fun `clicking remove button calls onRemoveClick`() {
    var removeClicked = false
    setContent(
      uiState =
        OfflineAreaViewerState(
          area = OFFLINE_AREA,
          areaName = OFFLINE_AREA.name,
          areaSize = "2.5",
        ),
      onRemoveClick = { removeClicked = true },
    )

    composeTestRule.onNodeWithTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG).performClick()

    assertTrue(removeClicked)
  }

  @Test
  fun `clicking back button calls onNavigateUp`() {
    var backClicked = false
    setContent(onNavigateUp = { backClicked = true })

    composeTestRule.onNodeWithContentDescription("Back").performClick()

    assertTrue(backClicked)
  }

  @Test
  fun `displays progress overlay when isProgressOverlayVisible is true`() {
    setContent(uiState = OfflineAreaViewerState(isProgressOverlayVisible = true))

    composeTestRule.onNodeWithTag(OFFLINE_AREA_VIEWER_PROGRESS_OVERLAY_TEST_TAG).assertIsDisplayed()
  }

  @Test
  fun `hides progress overlay when isProgressOverlayVisible is false`() {
    setContent(uiState = OfflineAreaViewerState(isProgressOverlayVisible = false))

    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_PROGRESS_OVERLAY_TEST_TAG)
      .assertIsNotDisplayed()
  }

  @Test
  fun `hides area size and disables remove button when area is null`() {
    setContent(uiState = OfflineAreaViewerState(area = null, areaName = "", areaSize = null))

    composeTestRule.onNodeWithTag(OFFLINE_AREA_VIEWER_NAME_TEST_TAG).assertTextEquals("")
    composeTestRule.onNodeWithTag(OFFLINE_AREA_VIEWER_SIZE_TEST_TAG).assertIsNotDisplayed()
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG)
      .assertIsDisplayed()
      .assertIsNotEnabled()
  }

  @Test
  fun `remove button is disabled when isProgressOverlayVisible is true`() {
    setContent(
      uiState =
        OfflineAreaViewerState(
          area = OFFLINE_AREA,
          areaName = OFFLINE_AREA.name,
          areaSize = "2.5",
          isProgressOverlayVisible = true,
        )
    )

    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG)
      .assertIsDisplayed()
      .assertIsNotEnabled()
  }

  private fun setContent(
    uiState: OfflineAreaViewerState = OfflineAreaViewerState(),
    onRemoveClick: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme {
        OfflineAreaViewerScreen(
          uiState = uiState,
          onRemoveClick = onRemoveClick,
          onNavigateUp = onNavigateUp,
        )
      }
    }
  }
}
