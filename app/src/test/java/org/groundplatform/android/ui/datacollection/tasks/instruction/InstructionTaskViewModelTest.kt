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

package org.groundplatform.android.ui.datacollection.tasks.instruction

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.JOB
import org.groundplatform.android.FakeData.newTask
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class InstructionTaskViewModelTest : BaseHiltTest() {

  private val viewModel = InstructionTaskViewModel()

  @Test
  fun `Should have the correct action buttons in the proper order`() = runWithTestDispatcher {
    setupViewModel()
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    assertThat(states.map { it.action })
      .containsExactly(ButtonAction.PREVIOUS, ButtonAction.NEXT)
      .inOrder()
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
