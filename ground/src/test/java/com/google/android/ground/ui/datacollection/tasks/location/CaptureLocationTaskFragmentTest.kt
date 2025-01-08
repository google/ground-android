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
import com.google.android.ground.R
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.CaptureLocationTaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.system.LocationManager
import com.google.android.ground.ui.common.MapConfig
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
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

  @BindValue @Mock lateinit var locationManager: LocationManager
  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject lateinit var mapStateRepository: MapStateRepository
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
  private val lastLocationFlow = MutableSharedFlow<Location>(replay = 1)

  override fun setUp() {
    super.setUp()
    // TODO: Add unit tests when card is hidden due to current button click
    // Issue URL: https://github.com/google/ground-android/issues/2952
    mapStateRepository.isLocationLockEnabled = true
    whenever(locationManager.locationUpdates).thenReturn(lastLocationFlow)
  }

  @Test
  fun testHeader() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    hasTaskViewWithoutHeader(task.label)
  }

  @Test
  fun testDropPin() = runWithTestDispatcher {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)
    setupLocation()

    runner()
      .clickButton("Capture")
      .assertButtonIsEnabled("Next")
      .assertButtonIsEnabled("Undo", true)
      .assertButtonIsHidden("Capture")
      .assertInfoCardShown(
        fragment.getString(R.string.current_location),
        "10°0'0\" N 20°0'0\" E",
        "5m",
      )

    hasValue(TASK_DATA)
  }

  @Test
  fun testInfoCard_noValue() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    runner().assertInfoCardHidden()
  }

  @Test
  fun testUndo() = runWithTestDispatcher {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)
    setupLocation()

    runner()
      .clickButton("Capture")
      .clickButton("Undo", true)
      .assertButtonIsHidden("Next")
      .assertButtonIsEnabled("Capture")
      // Info card is still shown as it is bound to current location and not response.
      .assertInfoCardShown(
        fragment.getString(R.string.current_location),
        "10°0'0\" N 20°0'0\" E",
        "5m",
      )

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

  @Test
  fun testGetMapConfig() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    assertThat(fragment.captureLocationTaskMapFragmentProvider.get().getMapConfig())
      .isEqualTo(MapConfig(showOfflineImagery = true, allowGestures = false))
  }

  private suspend fun setupLocation() {
    val location =
      mock<Location>().apply {
        whenever(hasAltitude()).thenReturn(true)
        whenever(hasAccuracy()).thenReturn(true)
        whenever(longitude).thenReturn(LONGITUDE)
        whenever(latitude).thenReturn(LATITUDE)
        whenever(altitude).thenReturn(ALTITUDE)
        whenever(accuracy).thenReturn(ACCURACY.toFloat())
      }

    lastLocationFlow.emit(location)
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
