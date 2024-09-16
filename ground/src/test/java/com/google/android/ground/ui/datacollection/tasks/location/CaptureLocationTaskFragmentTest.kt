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
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.CaptureLocationTaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

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
      isRequired = false,
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

    viewModel.updateLocation(location, TIMESTAMP)

    runner()
      .clickButton("Capture")
      .assertButtonIsEnabled("Next")
      .assertButtonIsEnabled("Undo", true)
      .assertButtonIsHidden("Capture")

    hasValue(TASK_DATA)
  }

  @Test
  fun testInfoCard_noValue() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    infoCardHidden()
  }

  @Test
  fun testUndo() = runWithTestDispatcher {
    val location = setupLocation()
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    viewModel.updateLocation(location, timestampMillis = 1000L)

    runner()
      .clickButton("Capture")
      .clickButton("Undo", true)
      .assertButtonIsHidden("Next")
      .assertButtonIsEnabled("Capture")

    hasValue(null)
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    assertFragmentHasButtons(
      ButtonAction.PREVIOUS,
      ButtonAction.SKIP,
      ButtonAction.UNDO,
      ButtonAction.CAPTURE_LOCATION,
      ButtonAction.NEXT,
    )
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task.copy(isRequired = false))

    runner()
      .assertButtonIsHidden("Next")
      .assertButtonIsEnabled("Skip")
      .assertButtonIsHidden("Undo", true)
      .assertButtonIsEnabled("Capture")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task.copy(isRequired = true))

    runner()
      .assertButtonIsHidden("Next")
      .assertButtonIsHidden("Skip")
      .assertButtonIsHidden("Undo", true)
      .assertButtonIsEnabled("Capture")
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
    private const val TIMESTAMP = 1000L
    private val GEOMETRY = Point(Coordinates(LATITUDE, LONGITUDE))
    private val TASK_DATA = CaptureLocationTaskData(GEOMETRY, ALTITUDE, ACCURACY, TIMESTAMP)
  }
}
