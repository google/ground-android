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
package org.groundplatform.android.ui.datacollection.tasks.time

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.text.SimpleDateFormat
import java.util.Date
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
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimeTaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: TimeTaskViewModel
  private var lastButtonAction: ButtonAction? = null
  private val buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)

  private fun setupTaskScreen(task: Task, taskData: TaskData? = null) {
    lastButtonAction = null
    viewModel = TimeTaskViewModel()
    viewModel.initialize(
      job = JOB,
      task = task,
      taskData = taskData,
      taskPositionInterface =
        object : TaskPositionInterface {
          override fun isFirst() = false

          override fun isLastWithValue(taskData: TaskData?) = false
        },
      surveyId = "survey_id",
    )

    composeTestRule.setContent {
      TimeTaskScreen(
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

    composeTestRule.onNodeWithText("Time label").assertIsDisplayed()
  }

  @Test
  fun showsHintText_whenTimeIsNotPresent() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText(getExpectedTimeHint()).assertIsDisplayed()
  }

  @Test
  fun showsTime_whenTimeIsPresent() {
    val date = Date(FIXED_TIMESTAMP)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val expectedTimeText = DateFormat.getTimeFormat(context).format(date)

    setupTaskScreen(TASK, DateTimeTaskData(date.time))

    composeTestRule.onNodeWithText(expectedTimeText).assertIsDisplayed()
  }

  @Test
  fun showsTimePickerSheet_whenTextFieldIsClicked() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(TIME_TEXT_TEST_TAG).performClick()

    composeTestRule.onNodeWithText("Clear").assertExists()
    composeTestRule.onNodeWithText("OK").assertExists()
  }

  @Test
  fun invokesOnResponseCleared_whenClearButtonIsClicked() {
    setupTaskScreen(TASK)
    composeTestRule.onNodeWithTag(TIME_TEXT_TEST_TAG).performClick()

    composeTestRule.onNodeWithText("Clear").performClick()
    composeTestRule.waitForIdle()

    assertThat(viewModel.taskTaskData.value).isNull()
  }

  @Test
  fun invokesOnValueSelected_whenOKButtonIsClicked() {
    setupTaskScreen(TASK, DateTimeTaskData(FIXED_TIMESTAMP))
    composeTestRule.onNodeWithTag(TIME_TEXT_TEST_TAG).performClick()

    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.waitForIdle()

    val taskData = viewModel.taskTaskData.value as DateTimeTaskData
    assertThat(taskData).isNotNull()
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

  private fun getExpectedTimeHint(): String {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val timeFormat = DateFormat.getTimeFormat(context) as SimpleDateFormat
    val hint = timeFormat.toPattern().uppercase()
    return hint
  }

  companion object {
    private val JOB = Job(id = "job1")

    private val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.TIME,
        label = "Time label",
        isRequired = false,
      )

    private const val FIXED_TIMESTAMP = 1704067200000L // 2024-01-01
  }
}
