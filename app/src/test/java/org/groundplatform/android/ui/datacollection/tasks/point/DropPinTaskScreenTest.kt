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
package org.groundplatform.android.ui.datacollection.tasks.point

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.JOB
import org.groundplatform.android.R
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.getString
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.ButtonActionStateChecker
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.map.CameraPosition
import org.groundplatform.domain.model.submission.DropPinTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.Task
import org.groundplatform.testing.FakeDataGenerator.newTask
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DropPinTaskScreenTest : BaseHiltTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Inject lateinit var viewModel: DropPinTaskViewModel
  @Inject lateinit var localValueStore: LocalValueStore
  private lateinit var buttonActionStateChecker: ButtonActionStateChecker
  private var lastScreenAction: TaskScreenAction? = null

  @Before
  override fun setUp() {
    super.setUp()
    buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)
  }

  @Test
  fun `displays task correctly`() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText(TASK.label).assertIsDisplayed()
  }

  @Test
  fun `sets initial action buttons state when task is optional`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.DROP_PIN, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
    )
  }

  @Test
  fun `sets initial action buttons state when task is required`() {
    setupTaskScreen(TASK.copy(isRequired = true))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.DROP_PIN, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
    )
  }

  @Test
  fun `drop pin button works`() {
    setupTaskScreen(TASK)
    viewModel.updateCameraPosition(CameraPosition(Coordinates(10.0, 20.0)))

    buttonActionStateChecker.getNode(ButtonAction.DROP_PIN).performClick()

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.UNDO, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.DROP_PIN, isEnabled = false, isVisible = false),
    )

    assertThat(viewModel.taskTaskData.value)
      .isEqualTo(DropPinTaskData(Point(Coordinates(10.0, 20.0))))
  }

  @Test
  fun `undo works`() {
    setupTaskScreen(TASK)
    viewModel.updateCameraPosition(CameraPosition(Coordinates(10.0, 20.0)))

    buttonActionStateChecker.getNode(ButtonAction.DROP_PIN).performClick()
    buttonActionStateChecker.getNode(ButtonAction.UNDO).performClick()

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.DROP_PIN, isEnabled = true, isVisible = true),
    )

    assertThat(viewModel.taskTaskData.value).isNull()
  }

  @Test
  fun `shows instructions dialog when needed`() {
    viewModel.instructionsDialogShown = false

    setupTaskScreen(TASK)

    val tooltipText = getString(R.string.drop_a_pin_tooltip_text)
    composeTestRule.onNodeWithText(tooltipText).assertIsDisplayed()
  }

  @Test
  fun `Initializes with task data`() {
    val location = Point(Coordinates(10.0, 20.0))
    val taskData = DropPinTaskData(location)
    setupTaskScreen(TASK, taskData = taskData)

    assertThat(viewModel.features.value).hasSize(1)
    assertThat(viewModel.features.value.first().geometry).isEqualTo(location)
  }

  @Test
  fun `dismissDropPinInstructions updates state`() {
    setupTaskScreen(TASK)
    localValueStore.dropPinInstructionsShown = false

    viewModel.dismissDropPinInstructions()

    assertThat(localValueStore.dropPinInstructionsShown).isTrue()
  }

  private fun setupTaskScreen(
    task: Task,
    isFirst: Boolean = false,
    isLastWithValue: Boolean = false,
    taskData: TaskData? = null,
    viewModelToUse: DropPinTaskViewModel = viewModel,
  ) {
    lastScreenAction = null
    viewModelToUse.initialize(
      job = JOB,
      task = task,
      taskData = taskData,
      taskPositionInterface = createTaskPositionInterface(isFirst, isLastWithValue),
      surveyId = "survey_id",
    )

    composeTestRule.setContent {
      DropPinTaskScreen(
        viewModel = viewModelToUse,
        onFooterPositionUpdated = {},
        shouldShowLoiNameDialog = false,
        loiName = "",
        onAction = { action ->
          lastScreenAction = action
          if (action is TaskScreenAction.OnButtonClicked) {
            viewModelToUse.onButtonClick(action.action)
          }
        },
        mapContent = { /* Dummy content */ },
      )
    }
  }

  private fun createTaskPositionInterface(isFirst: Boolean, isLastWithValue: Boolean) =
    object : TaskPositionInterface {
      override fun isFirst() = isFirst

      override fun isLastWithValue(taskData: TaskData?) = isLastWithValue
    }

  companion object {
    private val TASK = newTask(type = Task.Type.DROP_PIN).copy(label = "Task for dropping a pin")
  }
}
