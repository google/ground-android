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

package com.google.android.ground.ui.offlineareas.selector

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.ui.map.Bounds
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import junit.framework.Assert.assertFalse
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DownloadProgressDialogTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Inject lateinit var viewModel: OfflineAreaSelectorViewModel
  @Inject lateinit var offlineAreaRepository: OfflineAreaRepository

  @Test
  fun downloadProgressDialog_DisplaysTitleCorrectly() {
    composeTestRule.setContent { DownloadProgressDialog(viewModel.downloadProgress.value!!, {}) }

    composeTestRule
      .onNodeWithText(
        composeTestRule.activity.getString(
          R.string.offline_map_imagery_download_progress_dialog_title,
          0,
        )
      )
      .assertIsDisplayed()
  }

  @Test
  fun downloadProgressDialog_DisplaysCorrectMessage() {
    composeTestRule.setContent { DownloadProgressDialog(viewModel.downloadProgress.value!!, {}) }

    composeTestRule
      .onNodeWithText(
        composeTestRule.activity.getString(
          R.string.offline_map_imagery_download_progress_dialog_message
        )
      )
      .assertIsDisplayed()
  }

  @Test
  fun downloadProgressDialog_CallsOnDismissOnDismissButtonClick() {
    var isDismissed = false

    composeTestRule.setContent {
      DownloadProgressDialog(viewModel.downloadProgress.value!!, { isDismissed = true })
    }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .performClick()

    assertTrue(isDismissed)
  }

  @Test
  fun `stopDownloading cancels active download and updates UI state`() = runWithTestDispatcher {
    composeTestRule.setContent { DownloadProgressDialog(viewModel.downloadProgress.value!!, {}) }

    val progressFlow = MutableStateFlow<Pair<Int, Int>>(Pair(0, 0))
    whenever(offlineAreaRepository.downloadTiles(Bounds(-10.0, -20.0, 10.0, 20.0)))
      .thenReturn(progressFlow)

    viewModel.onDownloadClick()
    advanceUntilIdle()

    progressFlow.emit(Pair(50, 100))
    assertEquals(0.5f, viewModel.downloadProgress.value)

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .performClick()
    progressFlow.emit(Pair(75, 100))

    assertNotEquals(0.75f, viewModel.downloadProgress.value)
    assertFalse(viewModel.isDownloadProgressVisible.value!!)
    assertNull(viewModel.downloadJob)
  }

  @Test
  fun downloadProgressDialog_DisplaysCorrectTitleForProgress() {
    viewModel.downloadProgress.value = 0.5f

    composeTestRule.setContent { DownloadProgressDialog(viewModel.downloadProgress.value!!, {}) }

    composeTestRule
      .onNodeWithText(
        composeTestRule.activity.getString(
          R.string.offline_map_imagery_download_progress_dialog_title,
          50,
        )
      )
      .assertIsDisplayed()
  }
}
