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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.flow.MutableSharedFlow
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
  private val uiEffects = MutableSharedFlow<DataCollectionUiEffect>()

  @Before
  fun setUp() {
    mockViewModel = mock(DataCollectionViewModel::class.java)
    mockFragment = mock(DataCollectionFragment::class.java)
    mockAdapterFactory = mock(DataCollectionViewPagerAdapterFactory::class.java)
    mockAdapter = mock(DataCollectionViewPagerAdapter::class.java)

    `when`(mockViewModel.uiState).thenReturn(uiState)
    `when`(mockViewModel.footerVerticalPosition).thenReturn(footerVerticalPosition)
    `when`(mockViewModel.showExitWarning).thenReturn(showExitWarning)
    `when`(mockViewModel.uiEffects).thenReturn(uiEffects)

    `when`(mockAdapterFactory.create(any(), any())).thenReturn(mockAdapter)
    `when`(mockFragment.viewPagerAdapterFactory).thenReturn(mockAdapterFactory)
  }

  private fun setContent() {
    composeTestRule.setContent {
      DataCollectionScreen(
        viewModel = mockViewModel,
        fragment = mockFragment,
        onValidationError = {},
        onExitConfirmed = {},
      )
    }
  }

  @Test
  fun `Displays toolbar title when ready`() {
    uiState.value = READY_STATE

    setContent()

    composeTestRule.onNodeWithText("Test Job").assertIsDisplayed()
    composeTestRule.onNodeWithText("Test LOI").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `Displays toolbar title when submitted`() {
    uiState.value = DataCollectionUiState.TaskSubmitted(null)

    setContent()
    composeTestRule.waitForIdle()

    val expectedTitle = getString(R.string.data_collection_complete)
    composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `Displays confirmation screen when submitted`() {
    uiState.value = DataCollectionUiState.TaskSubmitted(null)

    setContent()

    val expectedText = getString(R.string.data_collection_complete_details)
    composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
  }

  @Test
  fun `Progress bar updates correctly when navigating between tasks`() {
    uiState.value = READY_STATE.copy(position = TaskPosition(0, 0, 2))

    setContent()
    composeTestRule.waitForIdle()

    // First task (0/1 progress)

    composeTestRule
      .onNodeWithTag(DataCollectionScreenTestTags.PROGRESS_BAR)
      .assertRangeInfoEquals(ProgressBarRangeInfo(0.0f, 0.0f..1.0f))

    // Update state to next task
    uiState.value = READY_STATE.copy(position = TaskPosition(1, 1, 2))
    composeTestRule.waitForIdle()

    // Second task (1/1 progress = 1.0f)
    composeTestRule
      .onNodeWithTag(DataCollectionScreenTestTags.PROGRESS_BAR)
      .assertRangeInfoEquals(ProgressBarRangeInfo(1.0f, 0.0f..1.0f))
  }

  @Test
  fun `Displays loading indicator when loading`() {
    uiState.value = DataCollectionUiState.Loading

    setContent()

    composeTestRule
      .onNodeWithTag(DataCollectionScreenTestTags.LOADING_INDICATOR)
      .assertIsDisplayed()
  }

  @Test
  fun `Displays error message when error`() {
    val errorCode = DataCollectionErrorCode.SURVEY_LOAD_FAILED
    uiState.value = DataCollectionUiState.Error(errorCode)

    setContent()

    composeTestRule.onNodeWithTag(DataCollectionScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Error: $errorCode").assertIsDisplayed()
  }

  companion object {
    private val JOB = Job(id = "job1", name = "Test Job")
    private val READY_STATE =
      DataCollectionUiState.Ready(
        surveyId = "survey1",
        job = JOB,
        loiName = "Test LOI",
        tasks = emptyList(),
        isAddLoiFlow = false,
        currentTaskId = "task1",
        position = TaskPosition(0, 1, 1),
      )
  }
}
