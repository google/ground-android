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
package org.groundplatform.android.ui.datacollection.tasks

import androidx.fragment.app.Fragment
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FragmentScenarioRule
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** A minimal fragment used to provide a viewLifecycleOwner for testing. */
class TestVisibilityFragment : Fragment(android.R.layout.simple_list_item_1)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TaskFragmentExtensionsTest : BaseHiltTest() {
  @get:Rule val fragmentScenario = FragmentScenarioRule()

  @Mock lateinit var dataCollectionViewModel: DataCollectionViewModel

  private val isCurrentTaskActiveFlow = MutableStateFlow(false)
  private val taskId = "test_task"

  override fun setUp() {
    super.setUp()
    whenever(dataCollectionViewModel.isCurrentActiveTaskFlow(taskId))
      .thenReturn(isCurrentTaskActiveFlow)
  }

  @Test
  fun `block does not execute when task is inactive`() = runWithTestDispatcher {
    var blockExecuted = false

    fragmentScenario.launchFragmentInHiltContainer<TestVisibilityFragment> {
      launchWhenTaskVisible(dataCollectionViewModel, taskId) { blockExecuted = true }
    }

    advanceUntilIdle()
    assertThat(blockExecuted).isFalse()
  }

  @Test
  fun `block executes when task becomes active`() = runWithTestDispatcher {
    var blockExecuted = false

    fragmentScenario.launchFragmentInHiltContainer<TestVisibilityFragment> {
      launchWhenTaskVisible(dataCollectionViewModel, taskId) { blockExecuted = true }
    }

    isCurrentTaskActiveFlow.value = true
    advanceUntilIdle()

    assertThat(blockExecuted).isTrue()
  }

  @Test
  fun `block is cancelled when task becomes inactive`() = runWithTestDispatcher {
    var isRunning = false
    var executionCount = 0

    fragmentScenario.launchFragmentInHiltContainer<TestVisibilityFragment> {
      launchWhenTaskVisible(dataCollectionViewModel, taskId) {
        executionCount++
        isRunning = true
        try {
          awaitCancellation()
        } finally {
          isRunning = false
        }
      }
    }

    advanceUntilIdle()
    assertThat(isRunning).isFalse()

    // Activate the task - block should start running
    isCurrentTaskActiveFlow.value = true
    advanceUntilIdle()
    assertThat(isRunning).isTrue()
    assertThat(executionCount).isEqualTo(1)

    // Deactivate the task - block should be canceled
    isCurrentTaskActiveFlow.value = false
    advanceUntilIdle()
    assertThat(isRunning).isFalse()
  }

  @Test
  fun `block restarts when task becomes active again`() = runWithTestDispatcher {
    var executionCount = 0
    var isRunning = false

    fragmentScenario.launchFragmentInHiltContainer<TestVisibilityFragment> {
      launchWhenTaskVisible(dataCollectionViewModel, taskId) {
        executionCount++
        isRunning = true
        try {
          awaitCancellation()
        } finally {
          isRunning = false
        }
      }
    }

    // First activation
    isCurrentTaskActiveFlow.value = true
    advanceUntilIdle()
    assertThat(executionCount).isEqualTo(1)
    assertThat(isRunning).isTrue()

    // Deactivate
    isCurrentTaskActiveFlow.value = false
    advanceUntilIdle()
    assertThat(isRunning).isFalse()

    // Reactivate - block should restart
    isCurrentTaskActiveFlow.value = true
    advanceUntilIdle()
    assertThat(executionCount).isEqualTo(2)
    assertThat(isRunning).isTrue()
  }
}
