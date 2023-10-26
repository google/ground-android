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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.job.Job
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
  private val job = Job(id = "job1")

  @Test
  fun testHeader() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    hasTaskViewWithoutHeader(task.label)
  }

  @Test
  fun testDropPin() = runWithTestDispatcher {
    val location = setupLocation()
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    viewModel.updateLocation(location)
    onView(withText("Capture")).perform(click())

    hasTaskData(TASK_DATA)
    buttonIsEnabled("Next")
    buttonIsEnabled(ButtonAction.UNDO)
    buttonIsHidden("Capture")
  }

  @Test
  fun testInfoCard_noTaskData() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    infoCardHidden()
  }

  @Test
  fun testUndo() = runWithTestDispatcher {
    val location = setupLocation()
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    viewModel.updateLocation(location)
    onView(withText("Capture")).perform(click())
    getButton(ButtonAction.UNDO).performClick()

    hasTaskData(null)
    buttonIsHidden("Next")
    buttonIsEnabled("Capture")
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    hasButtons(
      ButtonAction.NEXT,
      ButtonAction.SKIP,
      ButtonAction.UNDO,
      ButtonAction.CAPTURE_LOCATION
    )
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task.copy(isRequired = false))

    buttonIsHidden("Next")
    buttonIsEnabled("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Capture")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task.copy(isRequired = true))

    buttonIsHidden("Next")
    buttonIsHidden("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Capture")
  }

  private fun setupLocation(): Location =
    mock<Location>().apply {
      whenever(hasAltitude()).thenReturn(true)
      whenever(hasAccuracy()).thenReturn(true)
      whenever(longitude).thenReturn(LONGITUDE)
      whenever(latitude).thenReturn(LATITUDE)
      whenever(altitude).thenReturn(ALTITUDE)
      whenever(accuracy).thenReturn(ACCURACY.toFloat())
    }

  companion object {
    private const val LATITUDE = 10.0
    private const val LONGITUDE = 20.0
    private const val ACCURACY = 5.0
    private const val ALTITUDE = 150.0
    private val GEOMETRY = Point(Coordinates(LATITUDE, LONGITUDE))
    private val TASK_DATA = LocationTaskData(GEOMETRY, ALTITUDE, ACCURACY)
  }
}
