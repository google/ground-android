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
package org.groundplatform.android.ui.datacollection.tasks.instruction

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.components.TEST_TAG_TASK_VIEW_HEADER
import org.groundplatform.android.ui.datacollection.tasks.ButtonActionStateChecker
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.Task
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InstructionTaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private var lastScreenAction: TaskScreenAction? = null
  private val buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)

  private fun setupTaskScreen(
    task: Task,
    taskData: TaskData? = null,
    isFirst: Boolean = false,
    isLastWithValue: Boolean = false,
  ) {
    lastScreenAction = null
    val viewModel =
      InstructionTaskViewModel().apply {
        initialize(
          job = JOB,
          task = task,
          taskData = taskData,
          taskPositionInterface =
            object : TaskPositionInterface {
              override fun isFirst() = isFirst

              override fun isLastWithValue(taskData: TaskData?) = isLastWithValue
            },
        )
      }

    composeTestRule.setContent {
      InstructionTaskScreen(
        viewModel = viewModel,
        onFooterPositionUpdated = {},
        onAction = { lastScreenAction = it },
      )
    }
  }

  @Test
  fun `action buttons are enabled and visible by default`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
    )
  }

  @Test
  fun `instructions text is displayed`() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText("Instruction label").assertIsDisplayed()
  }

  @Test
  fun `invokes onAction when next button is clicked`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.getNode(ButtonAction.NEXT).performClick()

    assertThat(lastScreenAction).isEqualTo(TaskScreenAction.OnButtonClicked(ButtonAction.NEXT))
  }

  @Test
  fun `invokes onAction when previous button is clicked`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.getNode(ButtonAction.PREVIOUS).performClick()

    assertThat(lastScreenAction).isEqualTo(TaskScreenAction.OnButtonClicked(ButtonAction.PREVIOUS))
  }

  @Test
  fun `previous button is disabled when task is first`() {
    setupTaskScreen(TASK, isFirst = true)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = false, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
    )
  }

  @Test
  fun `done button is displayed when task is last`() {
    setupTaskScreen(TASK, isLastWithValue = true)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.DONE, isEnabled = true, isVisible = true),
    )
  }

  @Test
  fun `header component is missing`() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithTag(TEST_TAG_TASK_VIEW_HEADER).assertDoesNotExist()
  }

  companion object {
    private val JOB = Job(id = "job1")

    private val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.INSTRUCTIONS,
        label = "Instruction label",
        isRequired = true,
      )
  }
}
