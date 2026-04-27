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
package org.groundplatform.android.ui.datacollection.tasks.text

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.R
import org.groundplatform.android.common.Constants
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.ButtonActionStateChecker
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.submission.TextTaskData
import org.groundplatform.domain.model.task.Task
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextTaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: TextTaskViewModel
  private var lastButtonAction: ButtonAction? = null
  private val buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)

  private fun setupTaskScreen(task: Task, taskData: TaskData? = null) {
    lastButtonAction = null
    viewModel = TextTaskViewModel()
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
      TextTaskScreen(
        viewModel = viewModel,
        onFooterPositionUpdated = {},
        onButtonClicked = { lastButtonAction = it },
      )
    }
  }

  @Test
  fun displaysTaskHeader_whenTaskIsLoaded() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText("Text label").assertIsDisplayed()
  }

  @Test
  fun showsEmptyText_whenDefaultIsEmpty() {
    setupTaskScreen(TASK)

    composeTestRule
      .onNodeWithTag(INPUT_TEXT_TEST_TAG)
      .assertTextEquals("", "0 / ${Constants.TEXT_DATA_CHAR_LIMIT}")
  }

  @Test
  fun updatesValue_whenTextIsInserted() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).performTextInput("some text")

    assertThat(viewModel.taskTaskData.value).isEqualTo(TextTaskData("some text"))
  }

  @Test
  fun deletingTextResetsDisplayedTextAndNextButton() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).performTextInput("some text")
    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).performTextClearance()

    composeTestRule
      .onNodeWithTag(INPUT_TEXT_TEST_TAG)
      .assertTextEquals("", "0 / ${Constants.TEXT_DATA_CHAR_LIMIT}")
    assertThat(viewModel.taskTaskData.value).isNull()
  }

  @Test
  fun textOverCharacterLimitIsInvalid() {
    setupTaskScreen(TASK)

    composeTestRule
      .onNodeWithTag(INPUT_TEXT_TEST_TAG)
      .performTextInput("a".repeat(Constants.TEXT_DATA_CHAR_LIMIT + 1))

    assertThat(viewModel.validate()).isEqualTo(R.string.text_task_data_character_limit)
  }

  @Test
  fun enablesNextButton_whenTextIsEntered() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).performTextInput("Hello world")

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
    )
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
  fun invokesOnButtonClicked_whenActionButtonIsClicked() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.getNode(ButtonAction.PREVIOUS).performClick()

    assertThat(lastButtonAction).isEqualTo(ButtonAction.PREVIOUS)
  }

  companion object {
    private val JOB = Job(id = "job1")

    private val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.TEXT,
        label = "Text label",
        isRequired = false,
      )
  }
}
