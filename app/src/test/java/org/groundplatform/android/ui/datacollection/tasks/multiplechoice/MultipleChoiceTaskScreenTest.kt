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
package org.groundplatform.android.ui.datacollection.tasks.multiplechoice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.groundplatform.android.R
import org.groundplatform.android.common.Constants
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.ButtonActionStateChecker
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.submission.MultipleChoiceTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.MultipleChoice
import org.groundplatform.domain.model.task.Option
import org.groundplatform.domain.model.task.Task
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MultipleChoiceTaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: MultipleChoiceTaskViewModel
  private var lastButtonAction: ButtonAction? = null
  private val buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)

  private fun setupTaskScreen(
    task: Task,
    taskData: TaskData? = null,
    isFirst: Boolean = false,
    isLastWithValue: Boolean = false,
  ) {
    lastButtonAction = null
    viewModel = MultipleChoiceTaskViewModel()
    viewModel.initialize(
      job = JOB,
      task = task,
      taskData = taskData,
      taskPositionInterface =
        object : TaskPositionInterface {
          override fun isFirst() = isFirst

          override fun isLastWithValue(taskData: TaskData?) = isLastWithValue
        },
    )

    composeTestRule.setContent {
      MultipleChoiceTaskScreen(
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
  fun `displays task header when task is loaded`() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText("Text label").assertIsDisplayed()
  }

  @Test
  fun `renders select one options`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithText("Option 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Option 2").assertIsDisplayed()
  }

  @Test
  fun `renders select multiple options`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_MULTIPLE)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithText("Option 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Option 2").assertIsDisplayed()
  }

  @Test
  fun `allows only one selection for select one cardinality`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithText("Option 1").performClick()
    composeTestRule.onNodeWithText("Option 2").performClick()

    val taskData = viewModel.taskTaskData.value as MultipleChoiceTaskData
    assertThat(taskData.selectedOptionIds).containsExactly("option id 2")
  }

  @Test
  fun `allows multiple selection for select multiple cardinality`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_MULTIPLE)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithText("Option 1").performClick()
    composeTestRule.onNodeWithText("Option 2").performClick()

    val taskData = viewModel.taskTaskData.value as MultipleChoiceTaskData
    assertThat(taskData.selectedOptionIds).containsExactly("option id 1", "option id 2")
  }

  @Test
  fun `saves other text`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithText("Other").performClick()
    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextInput("User text")

    val taskData = viewModel.taskTaskData.value as MultipleChoiceTaskData
    assertThat(taskData.selectedOptionIds).containsExactly("[ User text ]")
  }

  @Test
  fun `sets initial action buttons state when task is optional`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `text over the character limit is invalid`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice))

    val userInput = "a".repeat(Constants.TEXT_DATA_CHAR_LIMIT + 1)
    composeTestRule.onNodeWithText("Other").performClick()
    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextInput(userInput)

    assertThat(viewModel.validate(TASK, viewModel.taskTaskData.value))
      .isEqualTo(R.string.text_task_data_character_limit)
  }

  @Test
  fun `selects other option on text input and deselects other radio inputs`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithText("Option 1").performClick()
    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextInput("A")

    val taskData = viewModel.taskTaskData.value as MultipleChoiceTaskData
    assertThat(taskData.selectedOptionIds).containsExactly("[ A ]")
  }

  @Test
  fun `deselects other option on text clear and required`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice, isRequired = true))

    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextInput("A")
    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextClearance()

    assertThat(viewModel.taskTaskData.value).isNull()
  }

  @Test
  fun `no deselection of other option on text clear when not required`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextInput("A")
    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextClearance()

    val taskData = viewModel.taskTaskData.value as MultipleChoiceTaskData
    assertThat(taskData.selectedOptionIds).containsExactly("[  ]")
  }

  @Test
  fun `no deselection of non-other selection when other is cleared`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice, isRequired = true))

    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextInput("A")
    composeTestRule.onNodeWithText("Option 1").performClick()
    composeTestRule.onNodeWithTag(OTHER_INPUT_TEXT_TEST_TAG).performTextInput("")

    val taskData = viewModel.taskTaskData.value as MultipleChoiceTaskData
    assertThat(taskData.selectedOptionIds).containsExactly("option id 1")
  }

  @Test
  fun `hides skip button when option is selected`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithText("Option 1").performClick()

    buttonActionStateChecker.getNode(ButtonAction.SKIP).assertDoesNotExist()
  }

  @Test
  fun `sets initial action buttons state when task is required`() {
    setupTaskScreen(TASK.copy(isRequired = true))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `sets action buttons state when it is the first task`() {
    setupTaskScreen(TASK, isFirst = true)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = false, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `sets action buttons state when it is the last task`() {
    setupTaskScreen(TASK, isLastWithValue = true)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.DONE, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `sets action buttons state when data is pre-filled`() {
    val multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_ONE)
    val taskData = MultipleChoiceTaskData(multipleChoice, listOf("option id 1"))
    setupTaskScreen(TASK.copy(multipleChoice = multipleChoice), taskData = taskData)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
    )
  }

  companion object {
    private val JOB = Job(id = "job1")

    private val OPTIONS =
      persistentListOf(
        Option("option id 1", "code1", "Option 1"),
        Option("option id 2", "code2", "Option 2"),
      )

    private val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.MULTIPLE_CHOICE,
        label = "Text label",
        isRequired = false,
        multipleChoice = MultipleChoice(OPTIONS, MultipleChoice.Cardinality.SELECT_ONE),
      )
  }
}
