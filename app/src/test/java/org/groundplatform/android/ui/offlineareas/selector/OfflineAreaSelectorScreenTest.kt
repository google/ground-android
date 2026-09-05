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

package org.groundplatform.android.ui.offlineareas.selector

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.assertTrue
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.android.ui.components.LOCATION_LOCKED_TEST_TAG
import org.groundplatform.android.ui.components.LOCATION_NOT_LOCKED_TEST_TAG
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.groundplatform.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `displays toolbar title and cancel button`() {
    setContent()

    composeTestRule
      .onNodeWithText(getString(R.string.offline_area_selector_title))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_CANCEL_BUTTON_TEST_TAG)
      .assertIsDisplayed()
      .assertIsEnabled()
  }

  @Test
  fun `displays default state with disabled download button`() {
    setContent()

    composeTestRule.onNodeWithTag(OFFLINE_AREA_SELECTOR_BOTTOM_TEXT_TEST_TAG).assertTextEquals("")
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG)
      .assertIsDisplayed()
      .assertIsNotEnabled()
  }

  @Test
  fun `displays area size when BottomTextState is AreaSize and enables download button`() {
    val state =
      OfflineAreaSelectorState(
        bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaSize("5.0")
      )
    setContent(uiState = state)

    val expectedText = getString(R.string.selected_offline_area_size, "5.0")
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_BOTTOM_TEXT_TEST_TAG)
      .assertTextEquals(expectedText)
    composeTestRule.onNodeWithTag(OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG).assertIsEnabled()
  }

  @Test
  fun `displays area too large when BottomTextState is AreaTooLarge and disables download button`() {
    val state =
      OfflineAreaSelectorState(
        bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaTooLarge
      )
    setContent(uiState = state)

    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_BOTTOM_TEXT_TEST_TAG)
      .assertTextEquals(getString(R.string.selected_offline_area_too_large))
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG)
      .assertIsNotEnabled()
  }

  @Test
  fun `displays loading message when BottomTextState is Loading and disables download button`() {
    val state =
      OfflineAreaSelectorState(bottomTextState = OfflineAreaSelectorState.BottomTextState.Loading)
    setContent(uiState = state)

    val loadingSymbol = getString(R.string.offline_area_size_loading_symbol)
    val expectedText = getString(R.string.selected_offline_area_size, loadingSymbol)
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_BOTTOM_TEXT_TEST_TAG)
      .assertTextEquals(expectedText)
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG)
      .assertIsNotEnabled()
  }

  @Test
  fun `displays network error when BottomTextState is NetworkError and disables download button`() {
    val state =
      OfflineAreaSelectorState(
        bottomTextState = OfflineAreaSelectorState.BottomTextState.NetworkError
      )
    setContent(uiState = state)

    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_BOTTOM_TEXT_TEST_TAG)
      .assertTextEquals(getString(R.string.connect_to_download_message))
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG)
      .assertIsNotEnabled()
  }

  @Test
  fun `displays no imagery available when BottomTextState is NoImageryAvailable and disables download button`() {
    val state =
      OfflineAreaSelectorState(
        bottomTextState = OfflineAreaSelectorState.BottomTextState.NoImageryAvailable
      )
    setContent(uiState = state)

    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_BOTTOM_TEXT_TEST_TAG)
      .assertTextEquals(getString(R.string.no_imagery_available_for_area))
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG)
      .assertIsNotEnabled()
  }

  @Test
  fun `disables download button when download is InProgress even if bottomTextState is AreaSize`() {
    val state =
      OfflineAreaSelectorState(
        bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaSize("5.0"),
        downloadState = OfflineAreaSelectorState.DownloadState.InProgress(0.4f),
      )
    setContent(uiState = state)

    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG)
      .assertIsDisplayed()
      .assertIsNotEnabled()
  }

  @Test
  fun `clicking cancel button triggers onCancelClick callback`() {
    var cancelClicked = false
    setContent(onCancelClick = { cancelClicked = true })

    composeTestRule.onNodeWithTag(OFFLINE_AREA_SELECTOR_CANCEL_BUTTON_TEST_TAG).performClick()

    assertTrue(cancelClicked)
  }

  @Test
  fun `clicking download button triggers onDownloadClick callback`() {
    var downloadClicked = false
    val state =
      OfflineAreaSelectorState(
        bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaSize("5.0")
      )
    setContent(uiState = state, onDownloadClick = { downloadClicked = true })

    composeTestRule.onNodeWithTag(OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG).performClick()

    assertTrue(downloadClicked)
  }

  @Test
  fun `clicking location lock button triggers onLocationLockClick callback`() {
    var locationLockClicked = false
    setContent(
      locationLockIconType = MapFloatingActionButtonType.LocationNotLocked,
      onLocationLockClick = { locationLockClicked = true },
    )

    composeTestRule.onNodeWithTag(LOCATION_NOT_LOCKED_TEST_TAG).performClick()

    assertTrue(locationLockClicked)
  }

  @Test
  fun `displays location locked icon when locationLockIconType is LocationLocked`() {
    setContent(locationLockIconType = MapFloatingActionButtonType.LocationLocked())

    composeTestRule.onNodeWithTag(LOCATION_LOCKED_TEST_TAG).assertIsDisplayed()
  }

  @Test
  fun `displays DownloadProgressDialog when download is InProgress and cancel dismisses it`() {
    var stopDownloadingCalled = false
    val state =
      OfflineAreaSelectorState(
        downloadState = OfflineAreaSelectorState.DownloadState.InProgress(0.5f)
      )
    setContent(uiState = state, onStopDownloading = { stopDownloadingCalled = true })

    composeTestRule
      .onNodeWithText(getString(R.string.offline_map_imagery_download_progress_dialog_message))
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag(DOWNLOAD_PROGRESS_DIALOG_CANCEL_BUTTON_TEST_TAG).performClick()

    assertTrue(stopDownloadingCalled)
  }

  @Test
  fun `does not display DownloadProgressDialog when download is Idle`() {
    val state =
      OfflineAreaSelectorState(downloadState = OfflineAreaSelectorState.DownloadState.Idle)
    setContent(uiState = state)

    composeTestRule
      .onNodeWithText(getString(R.string.offline_map_imagery_download_progress_dialog_message))
      .assertIsNotDisplayed()
  }

  @Test
  fun `DownloadProgressDialog displays title correctly`() {
    composeTestRule.setContent { DownloadProgressDialog(0f, {}) }

    composeTestRule
      .onNodeWithText(getString(R.string.offline_map_imagery_download_progress_dialog_title, 0))
      .assertIsDisplayed()
  }

  @Test
  fun `DownloadProgressDialog displays correct message`() {
    composeTestRule.setContent { DownloadProgressDialog(0f, {}) }

    composeTestRule
      .onNodeWithText(getString(R.string.offline_map_imagery_download_progress_dialog_message))
      .assertIsDisplayed()
  }

  @Test
  fun `DownloadProgressDialog displays correct title for progress percentage`() {
    composeTestRule.setContent { DownloadProgressDialog(0.5f, {}) }

    composeTestRule
      .onNodeWithText(getString(R.string.offline_map_imagery_download_progress_dialog_title, 50))
      .assertIsDisplayed()
  }

  @Test
  fun `DownloadProgressDialog displays 100 percent when complete`() {
    composeTestRule.setContent { DownloadProgressDialog(1.0f, onDismiss = {}) }

    composeTestRule
      .onNodeWithText(getString(R.string.offline_map_imagery_download_progress_dialog_title, 100))
      .assertIsDisplayed()
  }

  private fun setContent(
    uiState: OfflineAreaSelectorState = OfflineAreaSelectorState(),
    locationLockIconType: MapFloatingActionButtonType =
      MapFloatingActionButtonType.LocationNotLocked,
    onDownloadClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onLocationLockClick: () -> Unit = {},
    onStopDownloading: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme {
        OfflineAreaSelectorScreen(
          uiState = uiState,
          locationLockIconType = locationLockIconType,
          onDownloadClick = onDownloadClick,
          onCancelClick = onCancelClick,
          onLocationLockClick = onLocationLockClick,
          onStopDownloading = onStopDownloading,
        )
      }
    }
  }
}
