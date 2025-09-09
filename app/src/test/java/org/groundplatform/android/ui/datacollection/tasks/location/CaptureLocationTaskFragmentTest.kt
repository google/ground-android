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
package org.groundplatform.android.ui.datacollection.tasks.location

import android.location.Location
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import org.groundplatform.android.FlakyTest
import org.groundplatform.android.R
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.ui.common.MapConfig
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.tasks.BaseTaskFragmentTest
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
  fun `displays task without header correctly`() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    hasTaskViewWithoutHeader(task.label)
  }

  @Test
  @FlakyTest
  fun `drop pin`() = runWithTestDispatcher {
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
  fun `info card when no value`() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task)

    runner().assertInfoCardHidden()
  }

  @Test
  fun `undo resets location data`() = runWithTestDispatcher {
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
  fun `displays correct action buttons`() {
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
  fun `action buttons when task is optional`() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task.copy(isRequired = false))

    runner()
      .assertButtonIsHidden("Next")
      .assertButtonIsEnabled("Skip")
      .assertButtonIsHidden("Undo", true)
      .assertButtonIsEnabled("Capture")
  }

  @Test
  fun `action buttons when task is required`() {
    setupTaskFragment<CaptureLocationTaskFragment>(job, task.copy(isRequired = true))

    runner()
      .assertButtonIsHidden("Next")
      .assertButtonIsHidden("Skip")
      .assertButtonIsHidden("Undo", true)
      .assertButtonIsEnabled("Capture")
  }

  @Test
  fun `get map config`() {
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
