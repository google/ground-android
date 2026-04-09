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
package org.groundplatform.android.ui.datacollection.tasks.number

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.ButtonActionStateChecker
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.submission.NumberTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.Task
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NumberTaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: NumberTaskViewModel
  private var lastButtonAction: ButtonAction? = null
  private lateinit var buttonActionStateChecker: ButtonActionStateChecker

  private fun setupTaskScreen(task: Task, taskData: TaskData? = null) {
    lastButtonAction = null
    viewModel = NumberTaskViewModel()
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

    buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)

    composeTestRule.setContent {
      NumberTaskScreen(
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

    composeTestRule.onNodeWithText("Number label").assertIsDisplayed()
  }

  @Test
  fun responseWhenDefaultIsEmpty() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(INPUT_NUMBER_TEST_TAG).assertTextEquals("0 / 255", "")
    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun responseWhenOnUserInputNextButtonIsEnabled() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(INPUT_NUMBER_TEST_TAG).performTextInput("123.1")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(INPUT_NUMBER_TEST_TAG).assertTextEquals("5 / 255", "123.1")
    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
    )

    val taskData = viewModel.taskTaskData.value as NumberTaskData
    assertThat(taskData.number).isEqualTo("123.1")
  }

  @Test
  fun deletingNumberResetsTheDisplayedTextAndNextButton() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(INPUT_NUMBER_TEST_TAG).performTextInput("129.2")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(INPUT_NUMBER_TEST_TAG).performTextClearance()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(INPUT_NUMBER_TEST_TAG).assertTextEquals("0 / 255", "")
    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
    assertThat(viewModel.taskTaskData.value).isNull()
  }

  @Test
  fun displaysPrepopulatedResponse_whenTaskHasData() {
    setupTaskScreen(TASK, NumberTaskData("123.4"))

    composeTestRule.onNodeWithTag(INPUT_NUMBER_TEST_TAG).assertTextEquals("5 / 255", "123.4")
    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
    )
  }

  @Test
  fun responseWhenOnUserInputIsInvalidText_NextButtonIsDisabled() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(INPUT_NUMBER_TEST_TAG).performTextInput("-")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(INPUT_NUMBER_TEST_TAG).assertTextEquals("0 / 255", "")
    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
    assertThat(viewModel.taskTaskData.value).isNull()
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

  companion object {
    private val JOB = Job(id = "job1")

    private val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.NUMBER,
        label = "Number label",
        isRequired = false,
      )
  }
}
