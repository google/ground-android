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
@file:OptIn(ExperimentalCoroutinesApi::class)

package org.groundplatform.android.ui.datacollection.tasks.point

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.JOB
import org.groundplatform.android.FakeData.newTask
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DropPinTaskViewModelTest : BaseHiltTest() {

  @Inject lateinit var viewModel: DropPinTaskViewModel

  override fun setUp() {
    super.setUp()
    setupViewModel()
  }

  @Test
  fun `Should have the correct action buttons in the proper order`() = runWithTestDispatcher {
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    assertThat(states.map { it.action })
      .containsExactly(
        ButtonAction.PREVIOUS,
        ButtonAction.SKIP,
        ButtonAction.UNDO,
        ButtonAction.DROP_PIN,
        ButtonAction.NEXT,
      )
      .inOrder()
  }

  @Test
  fun `DROP_PIN is visible and enabled when there's no data yet`() = runWithTestDispatcher {
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.DROP_PIN })) {
      assertTrue(isVisible)
      assertTrue(isEnabled)
    }
  }

  @Test
  fun `DROP_PIN button is hidden when pin is dropped`() = runWithTestDispatcher {
    viewModel.setValue(DropPinTaskData(Point(Coordinates(0.0, 0.0))))
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.DROP_PIN })) {
      assertFalse(isVisible)
    }
  }

  @Test
  fun `UNDO is hidden when there's no data yet`() = runWithTestDispatcher {
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.UNDO })) { assertFalse(isVisible) }
  }

  @Test
  fun `UNDO is visible and enabled when pin is dropped`() = runWithTestDispatcher {
    viewModel.setValue(DropPinTaskData(Point(Coordinates(0.0, 0.0))))
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.UNDO })) {
      assertTrue(isVisible)
      assertTrue(isEnabled)
    }
  }

  @Test
  fun `onButtonClick DROP_PIN drops a pin at the current location`() = runWithTestDispatcher {
    viewModel.updateCameraPosition(CameraPosition(Coordinates(1.0, 2.0)))
    advanceUntilIdle()

    viewModel.onButtonClick(ButtonAction.DROP_PIN)
    advanceUntilIdle()

    val data = viewModel.taskTaskData.value as DropPinTaskData
    assertThat(data.location.coordinates.lat).isEqualTo(1.0)
    assertThat(data.location.coordinates.lng).isEqualTo(2.0)
  }

  private fun setupViewModel(
    isTaskRequired: Boolean = false,
    isFirstTask: Boolean = false,
    isLastTaskWithValue: Boolean = false,
  ) {
    viewModel.initialize(
      job = JOB,
      task = newTask(isRequired = isTaskRequired),
      taskData = null,
      taskPositionInterface =
        object : TaskPositionInterface {
          override fun isFirst(): Boolean = isFirstTask

          override fun isLastWithValue(taskData: TaskData?): Boolean = isLastTaskWithValue
        },
    )
  }
}
