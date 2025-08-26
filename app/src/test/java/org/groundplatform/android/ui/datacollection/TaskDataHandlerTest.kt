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
package org.groundplatform.android.ui.datacollection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.FlakyTest
import org.groundplatform.android.FlakyTestRule
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.TextTaskData
import org.groundplatform.android.model.task.Task
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDataHandlerTest {

  @get:Rule val flakyTestRule = FlakyTestRule()

  @Test
  fun `setData updates dataState correctly`() = runTest {
    val handler = TaskDataHandler()
    val task = createTask("task1")
    val taskData = createTaskData("data1")

    handler.setData(task, taskData)

    val dataState = handler.dataState.first()
    assertThat(dataState).hasSize(1)
    assertThat(dataState[task]).isEqualTo(taskData)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `dataState emits when value is updated`() = runTest {
    val handler = TaskDataHandler()
    val task = createTask("task1")
    val taskData1 = createTaskData("data1")
    val taskData2 = createTaskData("data2")

    val emissions = mutableListOf<Map<Task, TaskData?>>()
    val job = launch(UnconfinedTestDispatcher()) { handler.dataState.toList(emissions) }

    handler.setData(task, taskData1)
    handler.setData(task, taskData2)

    // Verify that both updates were emitted
    assertThat(emissions).hasSize(3)
    assertThat(emissions[0]).isEqualTo(emptyMap<Task, TaskData>())
    assertThat(emissions[1]).isEqualTo(mapOf(task to taskData1))
    assertThat(emissions[2]).isEqualTo(mapOf(task to taskData2))

    job.cancel()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  @FlakyTest
  fun `dataState does not emit when same value is set`() = runTest {
    val handler = TaskDataHandler()
    val task = createTask("task1")
    val taskData = createTaskData("data1")

    val emissions = mutableListOf<Map<Task, TaskData?>>()
    val job = launch(UnconfinedTestDispatcher()) { handler.dataState.toList(emissions) }

    handler.setData(task, taskData)
    handler.setData(task, taskData) // Same value set again

    assertThat(emissions).hasSize(2)
    assertThat(emissions[0]).isEqualTo(emptyMap<Task, TaskData>())
    assertThat(emissions[1]).isEqualTo(mapOf(task to taskData))

    job.cancel()
  }

  @Test
  @FlakyTest
  fun `getData returns correct data`() = runTest {
    val handler = TaskDataHandler()
    val task = createTask("task1")
    val taskData = createTaskData("data1")

    handler.setData(task, taskData)

    assertThat(handler.getData(task)).isEqualTo(taskData)
  }

  @Test
  @FlakyTest
  fun `getData returns null for unknown task`() = runTest {
    val handler = TaskDataHandler()
    val task = createTask("task1")

    assertThat(handler.getData(task)).isNull()
  }

  @Test
  fun `getTaskSelections returns correct values`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val task2 = createTask("task2")
    val task3 = createTask("task3")
    val taskData1 = createTaskData("data1")
    val taskData2 = createTaskData("data2")

    handler.setData(task1, taskData1)
    handler.setData(task2, taskData2)
    handler.setData(task3, null)

    val selections = handler.getTaskSelections()
    assertThat(selections).hasSize(2)
    assertThat(selections["task1"]).isEqualTo(taskData1)
    assertThat(selections["task2"]).isEqualTo(taskData2)
  }

  @Test
  fun `getTaskSelections with override returns correct values`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val task2 = createTask("task2")
    val taskData1 = createTaskData("data1")
    val taskData2 = createTaskData("data2")
    val taskDataOverride = createTaskData("override")

    handler.setData(task1, taskData1)
    handler.setData(task2, taskData2)

    val selections = handler.getTaskSelections(Pair("task1", taskDataOverride))
    assertThat(selections).hasSize(2)
    assertThat(selections["task1"]).isEqualTo(taskDataOverride)
    assertThat(selections["task2"]).isEqualTo(taskData2)
  }

  @Test
  fun `getTaskSelections with override having new value returns correct values`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val task2 = createTask("task2")
    val taskData1 = createTaskData("data1")
    val taskData2 = createTaskData("data2")
    val taskOverride = createTask("overrideTask")
    val taskDataOverride = createTaskData("overrideValue")

    handler.setData(task1, taskData1)
    handler.setData(task2, taskData2)

    val selections = handler.getTaskSelections(Pair(taskOverride.id, taskDataOverride))
    assertThat(selections).hasSize(3)
    assertThat(selections["task1"]).isEqualTo(taskData1)
    assertThat(selections["task2"]).isEqualTo(taskData2)
    assertThat(selections["overrideTask"]).isEqualTo(taskDataOverride)
  }

  @Test
  fun `getTaskSelections with null override returns correct selections`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val task2 = createTask("task2")
    val taskData1 = createTaskData("data1")
    val taskData2 = createTaskData("data2")

    handler.setData(task1, taskData1)
    handler.setData(task2, taskData2)

    val selections = handler.getTaskSelections(Pair("task1", null))
    assertThat(selections).hasSize(1)
    assertThat(selections["task2"]).isEqualTo(taskData2)
    assertThat(selections["task1"]).isNull()
  }

  @Test
  fun `setData with null value`() = runTest {
    val handler = TaskDataHandler()
    val task = createTask("task1")
    val taskData = createTaskData("data1")

    handler.setData(task, taskData)
    handler.setData(task, null)

    val dataState = handler.dataState.first()
    assertThat(dataState).hasSize(1)
    assertThat(dataState[task]).isNull()
  }

  private fun createTask(taskId: String): Task =
    FakeData.newTask(id = taskId, type = Task.Type.TEXT)

  private fun createTaskData(value: String): TextTaskData = TextTaskData(value)
}
