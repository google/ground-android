/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.geometry

import android.location.Location
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.task.DrawGeometry
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.LocationLockEnabledState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DrawGeometryTaskViewModelTest : BaseHiltTest() {

  @Mock lateinit var localValueStore: LocalValueStore
  @Mock lateinit var job: Job
  @Mock lateinit var uuidGenerator: OfflineUuidGenerator

  private lateinit var viewModel: DrawGeometryTaskViewModel

  @Before
  override fun setUp() {
    super.setUp()
    viewModel = DrawGeometryTaskViewModel(uuidGenerator, localValueStore)
  }

  @Test
  fun testLocationLockRequired_TaskConfigTrue_ReturnsTrue() = runWithTestDispatcher {
    `when`(uuidGenerator.generateUuid()).thenReturn("uuid")

    val task =
      Task("id", 0, Task.Type.DRAW_GEOMETRY, "label", false, drawGeometry = DrawGeometry(true, 10f))
    viewModel.initialize(job, task, null)

    assertThat(viewModel.isLocationLockRequired()).isTrue()
    // Should enable location lock
    assertThat(viewModel.enableLocationLockFlow.value).isEqualTo(LocationLockEnabledState.ENABLE)
  }

  @Test
  fun testLocationLockRequired_TaskConfigFalse_ReturnsFalse() = runWithTestDispatcher {
    `when`(uuidGenerator.generateUuid()).thenReturn("uuid")
    val task =
      Task(
        "id",
        0,
        Task.Type.DRAW_GEOMETRY,
        "label",
        false,
        drawGeometry = DrawGeometry(false, 10f),
      )
    viewModel.initialize(job, task, null)

    assertThat(viewModel.isLocationLockRequired()).isFalse()
    // Should NOT enable location lock automatically
    assertThat(viewModel.enableLocationLockFlow.value).isNotEqualTo(LocationLockEnabledState.ENABLE)
  }

  @Test
  fun testOnCaptureLocation_UpdatesValue() = runWithTestDispatcher {
    `when`(uuidGenerator.generateUuid()).thenReturn("uuid")
    val task =
      Task(
        "id",
        0,
        Task.Type.DRAW_GEOMETRY,
        "label",
        false,
        drawGeometry = DrawGeometry(true, 100f),
      )
    viewModel.initialize(job, task, null)

    val location =
      Location("test").apply {
        latitude = 10.0
        longitude = 20.0
        accuracy = 5f
      }
    viewModel.updateLocation(location)
    viewModel.onCaptureLocation()

    val taskData = viewModel.taskTaskData.value as CaptureLocationTaskData
    assertThat(taskData.location.coordinates).isEqualTo(Coordinates(10.0, 20.0))
    assertThat(taskData.accuracy).isEqualTo(5.0)
  }

  @Test
  fun testOnDropPin_UpdatesValue() = runWithTestDispatcher {
    `when`(uuidGenerator.generateUuid()).thenReturn("uuid")
    val task =
      Task(
        "id",
        0,
        Task.Type.DRAW_GEOMETRY,
        "label",
        false,
        drawGeometry = DrawGeometry(false, 10f),
      )
    viewModel.initialize(job, task, null)

    val cameraPosition = CameraPosition(Coordinates(10.0, 20.0), 10f)
    viewModel.updateCameraPosition(cameraPosition)
    viewModel.onDropPin()

    val taskData = viewModel.taskTaskData.value as DropPinTaskData
    assertThat(taskData.location.coordinates).isEqualTo(Coordinates(10.0, 20.0))
  }
}
