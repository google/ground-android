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
package org.groundplatform.android.ui.datacollection

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.flow.MutableStateFlow
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.domain.model.job.Job
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataCollectionScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockViewModel: DataCollectionViewModel
  private lateinit var mockFragment: DataCollectionFragment
  private lateinit var mockAdapterFactory: DataCollectionViewPagerAdapterFactory
  private lateinit var mockAdapter: DataCollectionViewPagerAdapter

  private val uiState = MutableStateFlow<DataCollectionUiState>(DataCollectionUiState.Loading)
  private val footerVerticalPosition = MutableStateFlow(0f)
  private val showExitWarning = MutableStateFlow(false)

  @Before
  fun setUp() {
    mockViewModel = mock(DataCollectionViewModel::class.java)
    mockFragment = mock(DataCollectionFragment::class.java)
    mockAdapterFactory = mock(DataCollectionViewPagerAdapterFactory::class.java)
    mockAdapter = mock(DataCollectionViewPagerAdapter::class.java)

    `when`(mockViewModel.uiState).thenReturn(uiState)
    `when`(mockViewModel.footerVerticalPosition).thenReturn(footerVerticalPosition)
    `when`(mockViewModel.showExitWarning).thenReturn(showExitWarning)

    `when`(mockAdapterFactory.create(any(), any())).thenReturn(mockAdapter)
    `when`(mockFragment.viewPagerAdapterFactory).thenReturn(mockAdapterFactory)
  }

  @Test
  fun `Displays toolbar title when ready`() {
    val job = Job(id = "job1", name = "Test Job")
    uiState.value =
      DataCollectionUiState.Ready(
        "survey1",
        job,
        "Test LOI",
        emptyList(),
        false,
        "task1",
        TaskPosition(0, 1, 1),
      )

    composeTestRule.setContent {
      DataCollectionScreen(viewModel = mockViewModel, fragment = mockFragment, onExitConfirmed = {})
    }

    composeTestRule.onNodeWithText("Test Job").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test LOI").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `Displays toolbar title when submitted`() {
    uiState.value = DataCollectionUiState.TaskSubmitted(null)

    composeTestRule.setContent {
      DataCollectionScreen(viewModel = mockViewModel, fragment = mockFragment, onExitConfirmed = {})
    }
    composeTestRule.waitForIdle()

    val expectedTitle = getString(R.string.data_collection_complete)
    composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `Displays confirmation screen when submitted`() {
    uiState.value = DataCollectionUiState.TaskSubmitted(null)

    composeTestRule.setContent {
      DataCollectionScreen(viewModel = mockViewModel, fragment = mockFragment, onExitConfirmed = {})
    }

    val expectedText = getString(R.string.data_collection_complete_details)
    composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
  }

  @Test
  fun `Progress bar updates correctly when navigating between tasks`() {
    val job = Job(id = "job1", name = "Test Job")
    uiState.value =
      DataCollectionUiState.Ready(
        "survey1",
        job,
        "Test LOI",
        emptyList(),
        false,
        "task1",
        TaskPosition(0, 0, 2),
      )

    composeTestRule.setContent {
      DataCollectionScreen(viewModel = mockViewModel, fragment = mockFragment, onExitConfirmed = {})
    }
    composeTestRule.waitForIdle()

    // First task (0/1 progress)

    composeTestRule
      .onNodeWithTag("progress_bar")
      .assertRangeInfoEquals(ProgressBarRangeInfo(0.0f, 0.0f..1.0f))

    // Update state to next task
    uiState.value =
      DataCollectionUiState.Ready(
        "survey1",
        job,
        "Test LOI",
        emptyList(),
        false,
        "task1",
        TaskPosition(1, 1, 2),
      )
    composeTestRule.waitForIdle()

    // Second task (1/1 progress = 1.0f)
    composeTestRule
      .onNodeWithTag("progress_bar")
      .assertRangeInfoEquals(ProgressBarRangeInfo(1.0f, 0.0f..1.0f))
  }

  @Test
  fun `Displays loading indicator when loading`() {
    uiState.value = DataCollectionUiState.Loading

    composeTestRule.setContent {
      DataCollectionScreen(viewModel = mockViewModel, fragment = mockFragment, onExitConfirmed = {})
    }

    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
  }

  @Test
  fun `Displays error message when error`() {
    val errorCode = DataCollectionErrorCode.SURVEY_LOAD_FAILED
    uiState.value = DataCollectionUiState.Error(errorCode)

    composeTestRule.setContent {
      DataCollectionScreen(viewModel = mockViewModel, fragment = mockFragment, onExitConfirmed = {})
    }

    composeTestRule.onNodeWithTag("error_message").assertIsDisplayed()
    composeTestRule.onNodeWithText("Error: $errorCode").assertIsDisplayed()
  }
}
