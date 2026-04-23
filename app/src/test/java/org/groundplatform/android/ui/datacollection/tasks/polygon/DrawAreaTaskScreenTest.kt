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
package org.groundplatform.android.ui.datacollection.tasks.polygon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
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
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.Polygon
import org.groundplatform.domain.model.submission.DrawAreaTaskData
import org.groundplatform.domain.model.submission.DrawAreaTaskIncompleteData
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
class DrawAreaTaskScreenTest : BaseHiltTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Inject lateinit var viewModel: DrawAreaTaskViewModel
  @Inject lateinit var localValueStore: LocalValueStore
  private lateinit var buttonActionStateChecker: ButtonActionStateChecker
  private var lastScreenAction: TaskScreenAction? = null

  @Before
  override fun setUp() {
    super.setUp()
    buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)
  }

  @Test
  fun `displays task header correctly`() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText(TASK.label).assertIsDisplayed()
  }

  @Test
  fun `sets initial action buttons state when task is optional`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = true),
      ButtonActionState(ButtonAction.REDO, isEnabled = false, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.ADD_POINT, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.COMPLETE, isEnabled = false, isVisible = false),
    )
  }

  @Test
  fun `sets initial action buttons state when task is required`() {
    setupTaskScreen(TASK.copy(isRequired = true))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = true),
      ButtonActionState(ButtonAction.REDO, isEnabled = false, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.ADD_POINT, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.COMPLETE, isEnabled = false, isVisible = false),
    )
  }

  @Test
  fun `draw area when incomplete when task is optional`() {
    setupTaskScreen(TASK.copy(isRequired = false))

    updateLastVertexAndAddPoint(COORDINATE_1)
    updateLastVertexAndAddPoint(COORDINATE_2)
    updateLastVertexAndAddPoint(COORDINATE_3)

    assertThat(viewModel.taskTaskData.value)
      .isEqualTo(
        DrawAreaTaskIncompleteData(
          LineString(
            listOf(
              Coordinates(0.0, 0.0),
              Coordinates(10.0, 10.0),
              Coordinates(20.0, 20.0),
              Coordinates(20.0, 20.0),
            )
          )
        )
      )

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.UNDO, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.REDO, isEnabled = false, isVisible = true),
      ButtonActionState(ButtonAction.ADD_POINT, isEnabled = false, isVisible = true),
      ButtonActionState(ButtonAction.COMPLETE, isEnabled = false, isVisible = false),
    )
  }

  @Test
  fun `draw area`() {
    setupTaskScreen(TASK.copy(isRequired = false))

    updateLastVertexAndAddPoint(COORDINATE_1)
    updateLastVertexAndAddPoint(COORDINATE_2)
    updateLastVertexAndAddPoint(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)

    buttonActionStateChecker.getNode(ButtonAction.COMPLETE).performClick()

    assertThat(viewModel.taskTaskData.value)
      .isEqualTo(
        DrawAreaTaskData(
          Polygon(
            LinearRing(
              listOf(
                Coordinates(0.0, 0.0),
                Coordinates(10.0, 10.0),
                Coordinates(20.0, 20.0),
                Coordinates(0.0, 0.0),
              )
            )
          )
        )
      )
  }

  @Test
  fun `draw area when add point button disabled when too close`() {
    setupTaskScreen(TASK.copy(isRequired = false))

    buttonActionStateChecker.getNode(ButtonAction.ADD_POINT).assertIsDisplayed()

    updateLastVertexAndAddPoint(COORDINATE_1)
    updateCloseVertex(COORDINATE_5)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.ADD_POINT, isEnabled = false, isVisible = true)
    )
  }

  @Test
  fun `redo button when is visible`() {
    setupTaskScreen(TASK.copy(isRequired = false))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.REDO, isEnabled = false, isVisible = true)
    )

    updateLastVertexAndAddPoint(COORDINATE_1)
    updateLastVertexAndAddPoint(COORDINATE_2)

    viewModel.removeLastVertex()

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.REDO, isEnabled = true, isVisible = true)
    )
  }

  @Test
  fun `redo button is enabled when redo stack is not empty even if vertices become empty`() {
    setupTaskScreen(TASK.copy(isRequired = false))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.REDO, isEnabled = false, isVisible = true)
    )

    updateLastVertexAndAddPoint(COORDINATE_1)
    updateLastVertexAndAddPoint(COORDINATE_2)

    viewModel.removeLastVertex()
    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.REDO, isEnabled = true, isVisible = true)
    )

    viewModel.removeLastVertex()
    viewModel.removeLastVertex()
    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.REDO, isEnabled = true, isVisible = true)
    )
  }

  @Test
  fun `Instructions dialog is shown`() {
    viewModel.instructionsDialogShown = false
    viewModel.showInstructions()

    setupTaskScreen(TASK)

    composeTestRule
      .onNodeWithText(getString(R.string.draw_area_task_instruction))
      .assertIsDisplayed()
  }

  @Test
  fun `Instructions dialog is not shown if shown previously`() {
    viewModel.instructionsDialogShown = true

    setupTaskScreen(TASK)

    composeTestRule
      .onNodeWithText(getString(R.string.draw_area_task_instruction))
      .assertIsNotDisplayed()
  }

  private fun setupTaskScreen(
    task: Task,
    isFirst: Boolean = false,
    isLastWithValue: Boolean = false,
    taskData: TaskData? = null,
    viewModelToUse: DrawAreaTaskViewModel = viewModel,
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
      DrawAreaTaskScreen(
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

  private fun updateLastVertexAndAddPoint(coordinate: Coordinates) {
    updateLastVertex(coordinate, false)
    buttonActionStateChecker.getNode(ButtonAction.ADD_POINT).performClick()
  }

  private fun updateLastVertex(coordinate: Coordinates, isNearFirstVertex: Boolean = false) {
    val threshold = PolygonDrawingSession.DISTANCE_THRESHOLD_DP.toDouble()
    val distanceInPixels = if (isNearFirstVertex) threshold else threshold + 1
    viewModel.updateLastVertexAndMaybeCompletePolygon(coordinate) { _, _ -> distanceInPixels }
  }

  private fun updateCloseVertex(coordinate: Coordinates) {
    val threshold = PolygonDrawingSession.DISTANCE_THRESHOLD_DP.toDouble()
    viewModel.updateLastVertexAndMaybeCompletePolygon(coordinate) { _, _ -> threshold }
  }
  companion object {
    private val TASK = newTask(type = Task.Type.DRAW_AREA).copy(label = "Task for drawing a polygon")
    private val COORDINATE_1 = Coordinates(0.0, 0.0)
    private val COORDINATE_2 = Coordinates(10.0, 10.0)
    private val COORDINATE_3 = Coordinates(20.0, 20.0)
    private val COORDINATE_4 = Coordinates(30.0, 30.0)
    private val COORDINATE_5 = Coordinates(5.0, 5.0)
  }
}
