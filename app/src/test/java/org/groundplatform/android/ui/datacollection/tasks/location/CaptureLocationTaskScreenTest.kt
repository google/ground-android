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

import android.content.Context
import android.location.Location
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.groundplatform.android.R
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.ButtonActionStateChecker
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.submission.CaptureLocationTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.Task
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CaptureLocationTaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testDispatcher = UnconfinedTestDispatcher()

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.CAPTURE_LOCATION,
      label = "Task for capturing current location",
      isRequired = false,
    )
  private val job = Job(id = "job1")
  private var lastScreenAction: TaskScreenAction? = null
  private var openSettingsCalled = false
  private lateinit var viewModel: CaptureLocationTaskViewModel
  private lateinit var buttonActionStateChecker: ButtonActionStateChecker
  private lateinit var context: Context

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    Dispatchers.setMain(testDispatcher)
    buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)
    context = ApplicationProvider.getApplicationContext()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun setupTaskScreen(
    task: Task,
    isFirst: Boolean = false,
    isLastWithValue: Boolean = false,
  ) {
    lastScreenAction = null
    viewModel = CaptureLocationTaskViewModel()
    viewModel.initialize(
      job = job,
      task = task,
      taskData = null,
      taskPositionInterface =
        object : TaskPositionInterface {
          override fun isFirst() = isFirst

          override fun isLastWithValue(taskData: TaskData?) = isLastWithValue
        },
      surveyId = "survey_id",
    )

    openSettingsCalled = false
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
        mapContent = { /* Dummy content */ }
      )
    }
  }

  @Test
  fun `displays task correctly`() {
    setupTaskScreen(task)

    composeTestRule.onNodeWithText(task.label).assertIsDisplayed()
  }

  @Test
  fun `drop pin`() = runTest(testDispatcher) {
    setupTaskScreen(task)
    viewModel.updateLocation(setupLocation())

    buttonActionStateChecker.getNode(ButtonAction.CAPTURE_LOCATION).performClick()

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.UNDO, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = false, isVisible = false),
    )

    assertThat(viewModel.taskTaskData.value).isEqualTo(TASK_DATA)
  }

  @Test
  fun `undo resets location data`() = runTest(testDispatcher) {
    setupTaskScreen(task)
    viewModel.updateLocation(setupLocation())

    buttonActionStateChecker.getNode(ButtonAction.CAPTURE_LOCATION).performClick()
    buttonActionStateChecker.getNode(ButtonAction.UNDO).performClick()

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = true, isVisible = true),
    )

    assertThat(viewModel.taskTaskData.value).isNull()
  }

  @Test
  fun `Initial action buttons state when task is optional`() = runTest(testDispatcher) {
    setupTaskScreen(task)
    viewModel.updateLocation(setupLocation(accuracy = 10.0))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
    )
  }

  @Test
  fun `Initial action buttons state when task is required`() = runTest(testDispatcher) {
    setupTaskScreen(task.copy(isRequired = true))
    viewModel.updateLocation(setupLocation(accuracy = 10.0))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.UNDO, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = false),
    )
  }

  @Test
  fun `capture button disabled when accuracy is poor`() = runTest(testDispatcher) {
    setupTaskScreen(task)
    viewModel.updateLocation(setupLocation(accuracy = 20.0))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = false, isVisible = true)
    )
  }

  @Test
  fun `capture button enabled when accuracy is good`() = runTest(testDispatcher) {
    setupTaskScreen(task)
    viewModel.updateLocation(setupLocation(accuracy = 10.0))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.CAPTURE_LOCATION, isEnabled = true, isVisible = true)
    )
  }

  @Test
  fun `accuracy card shown when accuracy is poor`() = runTest(testDispatcher) {
    setupTaskScreen(task)
    viewModel.updateLocation(setupLocation(accuracy = 25.0))

    val heading = context.getString(R.string.location_not_accurate_heading)
    composeTestRule.onNodeWithText(heading).assertIsDisplayed()
  }

  @Test
  fun `accuracy card hidden when accuracy is good`() = runTest(testDispatcher) {
    setupTaskScreen(task)
    viewModel.updateLocation(setupLocation(accuracy = 10.0))

    val heading = context.getString(R.string.location_not_accurate_heading)
    composeTestRule.onNodeWithText(heading).assertDoesNotExist()
  }

  private fun setupLocation(
    latitude: Double = LATITUDE,
    longitude: Double = LONGITUDE,
    accuracy: Double = ACCURACY,
    altitude: Double = ALTITUDE,
  ): Location {
    return mock<Location>().apply {
      whenever(hasAltitude()).thenReturn(true)
      whenever(hasAccuracy()).thenReturn(true)
      whenever(this.longitude).thenReturn(longitude)
      whenever(this.latitude).thenReturn(latitude)
      whenever(this.altitude).thenReturn(altitude)
      whenever(this.accuracy).thenReturn(accuracy.toFloat())
    }
  }

  companion object {
    private const val LATITUDE = 10.0
    private const val LONGITUDE = 20.0
    private const val ACCURACY = 5.0
    private const val ALTITUDE = 150.0
    private val GEOMETRY = Point(Coordinates(LATITUDE, LONGITUDE))
    private val TASK_DATA = CaptureLocationTaskData(GEOMETRY, ALTITUDE, ACCURACY)
  }
}
