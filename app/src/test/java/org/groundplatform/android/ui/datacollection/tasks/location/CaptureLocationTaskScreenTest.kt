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
package org.groundplatform.android.ui.datacollection.tasks.location

import android.location.Location
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.groundplatform.android.FakeData.JOB
import org.groundplatform.android.FakeData.newTask
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.ButtonActionStateChecker
import org.groundplatform.android.ui.datacollection.tasks.LocationLockEnabledState
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.submission.CaptureLocationTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.Task
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CaptureLocationTaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private var lastScreenAction: TaskScreenAction? = null
  private var openSettingsCalled = false
  private lateinit var viewModel: CaptureLocationTaskViewModel
  private lateinit var buttonActionStateChecker: ButtonActionStateChecker

  @Before
  fun setUp() {
    buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)
    viewModel = CaptureLocationTaskViewModel()
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
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
    )

    val heading = getString(R.string.location_not_accurate_heading)
    composeTestRule.onNodeWithText(heading).assertDoesNotExist()
  }

  @Test
  fun `sets initial action buttons state when task is required`() {
    setupTaskScreen(TASK.copy(isRequired = true))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
    )
  }

  @Test
  fun `disables capture button when accuracy is missing`() {
    setupTaskScreen(TASK, location = LOCATION_WITHOUT_ACCURACY)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = false, isVisible = true)
    )
  }

  @Test
  fun `handles poor accuracy`() {
    setupTaskScreen(TASK, location = BAD_LOCATION)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = false, isVisible = true)
    )

    val heading = getString(R.string.location_not_accurate_heading)
    composeTestRule.onNodeWithText(heading).assertIsDisplayed()
  }

  @Test
  fun `captures location when capture button is clicked`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.getNode(ButtonAction.CAPTURE_LOCATION).performClick()

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.UNDO, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = false, isVisible = false),
    )

    assertThat(viewModel.taskTaskData.value).isEqualTo(TASK_DATA)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `throws error when capture is attempted with poor accuracy`() {
    setupTaskScreen(TASK, location = BAD_LOCATION)

    viewModel.onButtonClick(ButtonAction.CAPTURE_LOCATION)
  }

  @Test
  fun `resets location data when undo is clicked`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.getNode(ButtonAction.CAPTURE_LOCATION).performClick()
    buttonActionStateChecker.getNode(ButtonAction.UNDO).performClick()

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = true, isVisible = true),
    )

    assertThat(viewModel.taskTaskData.value).isNull()
  }

  @Test
  fun `shows permission denied dialog when location lock needs enable`() {
    setupTaskScreen(TASK)

    viewModel.updateLocationLock(LocationLockEnabledState.NEEDS_ENABLE)

    val title = getString(R.string.allow_location_title)
    composeTestRule.onNodeWithText(title).assertIsDisplayed()
  }

  @Test
  fun `dismisses permission denied dialog and opens settings when allow is clicked`() {
    setupTaskScreen(TASK)

    viewModel.updateLocationLock(LocationLockEnabledState.NEEDS_ENABLE)

    val confirmButtonText = getString(R.string.allow_location_confirmation)
    composeTestRule.onNodeWithText(confirmButtonText).performClick()

    val title = getString(R.string.allow_location_title)
    composeTestRule.onNodeWithText(title).assertDoesNotExist()
    assertThat(openSettingsCalled).isTrue()
  }

  @Test
  fun `dismisses accuracy card when dismiss button is clicked`() {
    setupTaskScreen(TASK, location = BAD_LOCATION)

    val heading = getString(R.string.location_not_accurate_heading)
    composeTestRule.onNodeWithText(heading).assertIsDisplayed()

    composeTestRule.onNodeWithContentDescription("Close").performClick()

    composeTestRule.onNodeWithText(heading).assertDoesNotExist()
  }

  @Test
  fun `disables previous button when task is first`() {
    setupTaskScreen(TASK, isFirst = true)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = false, isVisible = true)
    )
  }

  @Test
  fun `handles last task with value`() {
    setupTaskScreen(TASK, isLastWithValue = true)

    buttonActionStateChecker.getNode(ButtonAction.CAPTURE_LOCATION).performClick()

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.DONE, isEnabled = true, isVisible = true)
    )
  }

  @Test
  fun `disables capture button when location is not available`() {
    setupTaskScreen(TASK, location = null)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = false, isVisible = true)
    )
  }

  private fun setupTaskScreen(
    task: Task,
    isFirst: Boolean = false,
    isLastWithValue: Boolean = false,
    location: Location? = GOOD_LOCATION,
  ) {
    lastScreenAction = null
    openSettingsCalled = false
    viewModel.initialize(
      job = JOB,
      task = task,
      taskData = null,
      taskPositionInterface = createTaskPositionInterface(isFirst, isLastWithValue),
      surveyId = "survey_id",
    )

    if (location != null) {
      viewModel.updateLocation(location)
    }

    composeTestRule.setContent {
      CaptureLocationTaskScreen(
        viewModel = viewModel,
        onFooterPositionUpdated = {},
        onAction = { action ->
          lastScreenAction = action
          if (action is TaskScreenAction.OnButtonClicked) {
            viewModel.onButtonClick(action.action)
          }
        },
        onOpenSettings = { openSettingsCalled = true },
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
    private const val LATITUDE = 10.0
    private const val LONGITUDE = 20.0
    private const val ALTITUDE = 150.0
    private val GEOMETRY = Point(Coordinates(LATITUDE, LONGITUDE))
    private val TASK_DATA = CaptureLocationTaskData(GEOMETRY, ALTITUDE, 10.0)
    private val TASK =
      newTask(type = Task.Type.CAPTURE_LOCATION).copy(label = "Task for capturing current location")

    private val GOOD_LOCATION =
      Location("gps").apply {
        latitude = LATITUDE
        longitude = LONGITUDE
        altitude = ALTITUDE
        accuracy = 10.0f
      }

    private val BAD_LOCATION =
      Location("gps").apply {
        latitude = LATITUDE
        longitude = LONGITUDE
        altitude = ALTITUDE
        accuracy = 20.0f
      }

    private val LOCATION_WITHOUT_ACCURACY =
      Location("gps").apply {
        latitude = LATITUDE
        longitude = LONGITUDE
        altitude = ALTITUDE
      }
  }
}
