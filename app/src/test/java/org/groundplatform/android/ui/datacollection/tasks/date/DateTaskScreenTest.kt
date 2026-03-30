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
package org.groundplatform.android.ui.datacollection.tasks.date

import android.content.Context
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionUiState
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.ButtonActionStateChecker
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.submission.DateTimeTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.Task
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DateTaskScreenTest : BaseHiltTest() {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @BindValue @Mock lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject lateinit var viewModelFactory: ViewModelFactory

  private lateinit var viewModel: DateTaskViewModel

  companion object {
    private val JOB = Job(id = "job1")
    private val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.DATE,
        label = "Date label",
        isRequired = false,
      )
    private const val FIXED_TIMESTAMP = 1704067200000L // 2024-01-01
  }

  private var lastButtonAction: ButtonAction? = null

  private val buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)

  private fun setupTaskScreen(task: Task, taskData: TaskData? = null) {
    lastButtonAction = null
    viewModel =
      viewModelFactory.create(DataCollectionViewModel.getViewModelClass(task.type))
        as DateTaskViewModel
    viewModel.initialize(
      job = JOB,
      task = task,
      taskData = taskData,
      taskPositionInterface =
        object : TaskPositionInterface {
          override fun isFirst() = false

          override fun isLastWithValue(taskData: TaskData?) = false
        },
    )
    whenever(dataCollectionViewModel.getTaskViewModel(task.id)).thenReturn(viewModel)
    whenever(dataCollectionViewModel.isCurrentActiveTaskFlow(task.id)).thenReturn(flowOf(true))
    whenever(dataCollectionViewModel.uiState)
      .thenReturn(MutableStateFlow(DataCollectionUiState.Loading))
    whenever(dataCollectionViewModel.loiNameDialogOpen).thenReturn(mutableStateOf(false))

    composeTestRule.setContent {
      DateTaskScreen(
        viewModel = viewModel,
        onFooterPositionUpdated = {},
        onAction = {
          if (it is TaskScreenAction.OnButtonClicked) {
            lastButtonAction = it.action
          }
        },
      )
    }
  }

  @Test
  fun displaysTaskHeader_whenTaskIsLoaded() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText("Date label").assertIsDisplayed()
  }

  @Test
  fun showsHintText_whenDateIsNotPresent() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hint = (DateFormat.getDateFormat(context) as SimpleDateFormat).toPattern().uppercase()

    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText(hint).assertIsDisplayed()
  }

  @Test
  fun showsHintText_whenTaskDataIsNotDateTimeTaskData() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hint = (DateFormat.getDateFormat(context) as SimpleDateFormat).toPattern().uppercase()
    val emptyTaskData =
      object : TaskData {
        override fun isEmpty(): Boolean = false
      }

    setupTaskScreen(TASK, emptyTaskData)

    composeTestRule.onNodeWithText(hint).assertIsDisplayed()
  }

  @Test
  fun showsDate_whenDateIsPresent() {
    val date = Date(FIXED_TIMESTAMP)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val expectedDateText = DateFormat.getDateFormat(context).format(date)

    setupTaskScreen(TASK, DateTimeTaskData(date.time))

    composeTestRule.onNodeWithText(expectedDateText).assertIsDisplayed()
  }

  @Test
  fun showsDatePickerSheet_whenTextFieldIsClicked() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(DATE_TEXT_TEST_TAG).performClick()

    composeTestRule.onNodeWithText("Clear").assertExists()
    composeTestRule.onNodeWithText("OK").assertExists()
  }

  @Test
  fun invokesOnResponseCleared_whenClearButtonIsClicked() {
    setupTaskScreen(TASK)
    composeTestRule.onNodeWithTag(DATE_TEXT_TEST_TAG).performClick()

    composeTestRule.onNodeWithText("Clear").performClick()
    composeTestRule.waitForIdle()

    assertThat(viewModel.taskTaskData.value).isNull()
  }

  @Test
  fun invokesOnDateSelected_whenOKButtonIsClicked() {
    setupTaskScreen(TASK, DateTimeTaskData(FIXED_TIMESTAMP))
    composeTestRule.onNodeWithTag(DATE_TEXT_TEST_TAG).performClick()

    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.waitForIdle()

    val taskData = viewModel.taskTaskData.value as DateTimeTaskData
    assertThat(taskData.timeInMillis).isEqualTo(FIXED_TIMESTAMP)
  }

  @Test
  fun setsInitialActionButtonsState_whenTaskIsOptional() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun setsInitialActionButtonsState_whenTaskIsRequired() {
    setupTaskScreen(TASK.copy(isRequired = true))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun setsActionButtonsState_whenDataIsPresent() {
    setupTaskScreen(TASK.copy(isRequired = true), DateTimeTaskData(FIXED_TIMESTAMP))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
    )
  }

  @Test
  fun invokesOnButtonClicked_whenActionButtonIsClicked() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.getNode(ButtonAction.PREVIOUS).performClick()

    assertThat(lastButtonAction).isEqualTo(ButtonAction.PREVIOUS)
  }
}
