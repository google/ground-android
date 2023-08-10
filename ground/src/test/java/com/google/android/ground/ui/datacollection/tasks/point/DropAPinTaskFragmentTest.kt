/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.point

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.submission.GeometryData
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import com.google.android.ground.ui.map.CameraPosition
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.core.IsNot.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DropAPinTaskFragmentTest :
  BaseTaskFragmentTest<DropAPinTaskFragment, DropAPinTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.DROP_A_PIN,
      label = "Task for dropping a pin",
      isRequired = false
    )

  @Test
  fun testHeader() {
    setupTaskFragment<DropAPinTaskFragment>(task)

    hasTaskViewWithoutHeader("Drop a pin")
  }

  @Test
  fun testDropPin() = runWithTestDispatcher {
    val testPosition = CameraPosition(Coordinates(10.0, 20.0))
    setupTaskFragment<DropAPinTaskFragment>(task)

    viewModel.updateCameraPosition(testPosition)
    onView(withText("Drop pin")).perform(click())

    hasTaskData(GeometryData(Point(Coordinates(10.0, 20.0))))
    buttonIsEnabled("Continue")
    buttonIsEnabled(ButtonAction.UNDO)
    buttonIsHidden("Drop pin")
  }

  @Test
  fun testInfoCard_noTaskData() {
    setupTaskFragment<DropAPinTaskFragment>(task)

    infoCardHidden()
  }

  @Test
  fun testInfoCard_withTaskData() {
    setupTaskFragment<DropAPinTaskFragment>(task)

    viewModel.updateCameraPosition(CameraPosition(Coordinates(10.0, 20.0)))
    onView(withText("Drop pin")).perform(click())

    infoCardShown("Dropped pin", "10°0'0\" N 20°0'0\" E")
  }

  @Test
  fun testUndo() = runWithTestDispatcher {
    val testPosition = CameraPosition(Coordinates(10.0, 20.0))
    setupTaskFragment<DropAPinTaskFragment>(task)

    viewModel.updateCameraPosition(testPosition)
    onView(withText("Drop pin")).perform(click())
    getButton(ButtonAction.UNDO).performClick()

    hasTaskData(null)
    buttonIsHidden("Continue")
    buttonIsEnabled("Drop pin")
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<DropAPinTaskFragment>(task)

    hasButtons(ButtonAction.CONTINUE, ButtonAction.SKIP, ButtonAction.UNDO, ButtonAction.DROP_PIN)
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<DropAPinTaskFragment>(task.copy(isRequired = false))

    buttonIsHidden("Continue")
    buttonIsEnabled("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Drop pin")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<DropAPinTaskFragment>(task.copy(isRequired = true))

    buttonIsHidden("Continue")
    buttonIsHidden("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Drop pin")
  }
}
