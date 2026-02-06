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

package org.groundplatform.android.ui.datacollection.tasks

import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.JOB
import org.groundplatform.android.FakeData.newTask
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.TextTaskData
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class AbstractTaskViewModelTest : BaseHiltTest() {

  private val viewModel =
    object : AbstractTaskViewModel() {
      // Minimal implementation that uses all default behavior
    }

  @Test
  fun `PREVIOUS button is disabled on the first task`() = runWithTestDispatcher {
    setupViewModel(isFirstTask = true)
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.PREVIOUS })) {
      assertTrue(isVisible)
      assertFalse(isEnabled)
    }
  }

  @Test
  fun `PREVIOUS button is enabled when not the first task`() = runWithTestDispatcher {
    setupViewModel(isFirstTask = false)
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.PREVIOUS })) {
      assertTrue(isVisible)
      assertTrue(isEnabled)
    }
  }

  @Test
  fun `SKIP button is visible and enabled when the task is optional and there is no data`() =
    runWithTestDispatcher {
      setupViewModel(isTaskRequired = false)
      advanceUntilIdle()

      val states = viewModel.taskActionButtonStates.first()

      with(requireNotNull(states.find { it.action == ButtonAction.SKIP })) {
        assertTrue(isVisible)
        assertTrue(isEnabled)
      }
    }

  @Test
  fun `SKIP button is hidden when the task is required`() = runWithTestDispatcher {
    setupViewModel(isTaskRequired = true)
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    assertFalse(states.find { it.action == ButtonAction.SKIP }!!.isVisible)
  }

  @Test
  fun `SKIP button is hidden when the task has data`() = runWithTestDispatcher {
    setupViewModel(isTaskRequired = false)
    viewModel.setValue(TextTaskData.fromString("test"))
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    assertFalse(states.find { it.action == ButtonAction.SKIP }!!.isVisible)
  }

  @Test
  fun `NEXT button is visible and disabled when there is no task data`() = runWithTestDispatcher {
    setupViewModel()
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.NEXT })) {
      assertTrue(isVisible)
      assertFalse(isEnabled)
    }
  }

  @Test
  fun `NEXT button is enabled when there is task data`() = runWithTestDispatcher {
    setupViewModel()
    viewModel.setValue(TextTaskData.fromString("test"))
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.NEXT })) {
      assertTrue(isVisible)
      assertTrue(isEnabled)
    }
  }

  @Test
  fun `DONE is shown instead of NEXT when it's the last task with data`() = runWithTestDispatcher {
    setupViewModel(isLastTaskWithValue = true)
    viewModel.setValue(TextTaskData.fromString("test"))
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.DONE })) {
      assertTrue(isVisible)
      assertTrue(isEnabled)
    }
    assertEquals(null, states.find { it.action == ButtonAction.NEXT })
  }

  @Test
  fun `NEXT is shown when it's the last task but there is no data yet`() = runWithTestDispatcher {
    setupViewModel(isLastTaskWithValue = false)
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.NEXT })) {
      assertTrue(isVisible)
      assertFalse(isEnabled)
    }
    assertEquals(null, states.find { it.action == ButtonAction.DONE })
  }

  @Test
  fun `onButtonClick UNDO clears the task data`() = runWithTestDispatcher {
    setupViewModel()
    viewModel.setValue(TextTaskData.fromString("test"))
    advanceUntilIdle()

    assertEquals("test", (viewModel.taskTaskData.value as TextTaskData).text)

    viewModel.onButtonClick(ButtonAction.UNDO)
    advanceUntilIdle()

    assertEquals(null, viewModel.taskTaskData.value)
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
