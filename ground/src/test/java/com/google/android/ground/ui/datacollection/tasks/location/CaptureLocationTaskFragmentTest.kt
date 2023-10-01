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
package com.google.android.ground.ui.datacollection.tasks.location

import android.location.Location
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.submission.LocationTaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CaptureLocationTaskFragmentTest :
  BaseTaskFragmentTest<CaptureLocationTaskFragment, CaptureLocationTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.CAPTURE_LOCATION,
      label = "Task for capturing current location",
      isRequired = false
    )

  @Test
  fun testHeader() {
    setupTaskFragment<CaptureLocationTaskFragment>(task)

    hasTaskViewWithoutHeader("Capture location")
  }

  @Test
  fun testDropPin() = runWithTestDispatcher {
    val location = setupLocation()
    setupTaskFragment<CaptureLocationTaskFragment>(task)

    viewModel.updateLocation(location)
    Espresso.onView(ViewMatchers.withText("Capture")).perform(ViewActions.click())

    hasTaskData(LocationTaskData(Point(Coordinates(10.0, 20.0)), 200.0, 10.0))
    buttonIsEnabled("Continue")
    buttonIsEnabled(ButtonAction.UNDO)
    buttonIsHidden("Capture")
  }

  @Test
  fun testInfoCard_noTaskData() {
    setupTaskFragment<CaptureLocationTaskFragment>(task)

    infoCardHidden()
  }

  @Test
  fun testUndo() = runWithTestDispatcher {
    val location = setupLocation()
    setupTaskFragment<CaptureLocationTaskFragment>(task)

    viewModel.updateLocation(location)
    Espresso.onView(ViewMatchers.withText("Capture")).perform(ViewActions.click())
    getButton(ButtonAction.UNDO).performClick()

    hasTaskData(null)
    buttonIsHidden("Continue")
    buttonIsEnabled("Capture")
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<CaptureLocationTaskFragment>(task)

    hasButtons(
      ButtonAction.CONTINUE,
      ButtonAction.SKIP,
      ButtonAction.UNDO,
      ButtonAction.CAPTURE_LOCATION
    )
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<CaptureLocationTaskFragment>(task.copy(isRequired = false))

    buttonIsHidden("Continue")
    buttonIsEnabled("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Capture")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<CaptureLocationTaskFragment>(task.copy(isRequired = true))

    buttonIsHidden("Continue")
    buttonIsHidden("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Capture")
  }

  private fun setupLocation(): Location =
    mock<Location>().apply {
      whenever(hasAltitude()).thenReturn(true)
      whenever(hasAccuracy()).thenReturn(true)
      whenever(longitude).thenReturn(20.0)
      whenever(latitude).thenReturn(10.0)
      whenever(altitude).thenReturn(200.0)
      whenever(accuracy).thenReturn(10.0f)
    }
}
