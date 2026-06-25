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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.MutableLiveData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.text.INPUT_TEXT_TEST_TAG
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskViewModel
import org.groundplatform.domain.model.task.Task
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataCollectionScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

  @Mock private lateinit var mockViewModel: DataCollectionViewModel

  private val uiState = MutableStateFlow<DataCollectionUiState>(DataCollectionUiState.Loading)
  private val showExitWarning = MutableStateFlow(false)
  private val uiEffects = MutableSharedFlow<DataCollectionUiEffect>()
  private val loiNameDraft = MutableStateFlow("")
  private val loiNameDialogOpen = MutableStateFlow(false)

  @Before
  fun setUp() {
    whenever(mockViewModel.uiState).doReturn(uiState)
    whenever(mockViewModel.showExitWarning).doReturn(showExitWarning)
    whenever(mockViewModel.uiEffects).doReturn(uiEffects)
    whenever(mockViewModel.loiNameDraft).doReturn(loiNameDraft)
    whenever(mockViewModel.loiNameDialogOpen).doReturn(loiNameDialogOpen)
  }

  private fun setContent(onValidationError: (Int) -> Unit = {}, onExitConfirmed: () -> Unit = {}) {
    composeTestRule.setContent {
      DataCollectionScreen(
        viewModel = mockViewModel,
        onValidationError = onValidationError,
        onExitConfirmed = onExitConfirmed,
        onOpenSettings = {},
        onAwaitingPhotoCapture = {},
        onReportExportError = {},
      )
    }
  }

  @Test
  fun `Displays toolbar title and subtitle when ready`() {
    uiState.value = READY_STATE

    setContent()

    composeTestRule.onNodeWithText(JOB.name!!).assertIsDisplayed()
    composeTestRule.onNodeWithText(LOI_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `Displays only toolbar title when LOI name is missing`() {
    uiState.value = READY_STATE.copy(loiName = "")

    setContent()

    composeTestRule.onNodeWithText(JOB.name!!).assertIsDisplayed()
    composeTestRule.onNodeWithText(LOI_NAME).assertDoesNotExist()
  }

  @Test
  fun `Displays confirmation screen on submission`() {
    uiState.value = DataCollectionUiState.TaskSubmitted(null)

    setContent()

    // Verify Toolbar
    composeTestRule.onNodeWithText(getString(R.string.data_collection_complete)).assertIsDisplayed()

    // Verify Content
    composeTestRule
      .onNodeWithText(getString(R.string.data_collection_complete_details))
      .assertIsDisplayed()
  }

  @Test
  fun `Displays exit warning dialog when triggered`() {
    uiState.value = READY_STATE
    showExitWarning.value = true

    setContent()

    composeTestRule
      .onNodeWithText(getString(R.string.data_collection_cancellation_title))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(getString(R.string.data_collection_cancellation_confirm_button))
      .assertIsDisplayed()
  }

  @Test
  fun `Calls onExitConfirmed when Exit effect is emitted`() = runTest {
    var exitConfirmedCalled = false
    setContent(onExitConfirmed = { exitConfirmedCalled = true })

    uiEffects.emit(DataCollectionUiEffect.Exit)
    composeTestRule.waitForIdle()

    assertThat(exitConfirmedCalled).isTrue()
  }

  @Test
  fun `Calls onValidationError when ShowValidationError effect is emitted`() = runTest {
    var validationErrorResId: Int? = null
    setContent(onValidationError = { validationErrorResId = it })

    val errorResId = R.string.close
    uiEffects.emit(DataCollectionUiEffect.ShowValidationError(errorResId))
    composeTestRule.waitForIdle()

    assertThat(validationErrorResId).isEqualTo(errorResId)
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
  fun `Displays error message when error occurs`() {
    val errorCode = DataCollectionErrorCode.SURVEY_LOAD_FAILED
    uiState.value = DataCollectionUiState.Error(errorCode)

    setContent()

    composeTestRule.onNodeWithTag(DataCollectionScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.unexpected_error)).assertIsDisplayed()
  }

  @Test
  fun `Displays correct task when absoluteIndex differs from relativeIndex`() {
    val task1 =
      Task(
        id = "task1",
        index = 0,
        type = Task.Type.INSTRUCTIONS,
        label = "Instructions Task",
        isRequired = false,
      )
    val task2 =
      Task(id = "task2", index = 1, type = Task.Type.TEXT, label = "Text Task", isRequired = true)

    val mockTextTaskViewModel = mock(TextTaskViewModel::class.java)
    val mockTaskActionButtonStates = MutableStateFlow<List<ButtonActionState>>(emptyList())
    val mockResponseText = MutableLiveData("")

    whenever(mockTextTaskViewModel.taskActionButtonStates).thenReturn(mockTaskActionButtonStates)
    whenever(mockTextTaskViewModel.responseText).thenReturn(mockResponseText)
    whenever(mockTextTaskViewModel.task).thenReturn(task2)

    whenever(mockViewModel.getTaskViewModel("task2")).thenReturn(mockTextTaskViewModel)

    uiState.value =
      READY_STATE.copy(
        tasks = listOf(task1, task2),
        currentTaskId = "task2",
        position = TaskPosition(absoluteIndex = 1, relativeIndex = 0, sequenceSize = 1),
      )

    setContent()

    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).assertIsDisplayed()
  }

  companion object {
    private val JOB = FakeData.JOB.copy(name = "Test Job")
    private const val LOI_NAME = "Test LOI"
    private val READY_STATE =
      DataCollectionUiState.Ready(
        surveyId = FakeData.SURVEY_ID,
        job = JOB,
        loiName = LOI_NAME,
        tasks = emptyList(),
        isAddLoiFlow = false,
        currentTaskId = "task1",
        position = TaskPosition(0, 0, 1),
      )
  }
}
