package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.task.Task
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDataHandlerTest {

  @Test
  fun `setData updates dataState correctly`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val taskData1 = createTaskData("data1")

    handler.setData(task1, taskData1)

    val dataState = handler.dataState.first()
    assertThat(dataState).hasSize(1)
    assertThat(dataState[task1]).isEqualTo(taskData1)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `dataState emits when value is updated`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val taskData1 = createTaskData("data1")
    val taskData2 = createTaskData("data2")

    val emittedStates = mutableListOf<Map<Task, TaskData?>>()
    val collectJob = launch(UnconfinedTestDispatcher()) { handler.dataState.toList(emittedStates) }

    handler.setData(task1, taskData1)
    handler.setData(task1, taskData2)

    assertThat(emittedStates).hasSize(3)
    assertThat(emittedStates[0]).isEqualTo(emptyMap<Task, TaskData>())
    assertThat(emittedStates[1]).isEqualTo(mapOf(task1 to taskData1))
    assertThat(emittedStates[2]).isEqualTo(mapOf(task1 to taskData2))

    collectJob.cancel()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `dataState does not emit when same value is set`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val taskData1 = createTaskData("data1")

    val emittedStates = mutableListOf<Map<Task, TaskData?>>()
    val collectJob = launch(UnconfinedTestDispatcher()) { handler.dataState.toList(emittedStates) }

    handler.setData(task1, taskData1)
    handler.setData(task1, taskData1)

    assertThat(emittedStates).hasSize(2)
    assertThat(emittedStates[0]).isEqualTo(emptyMap<Task, TaskData>())
    assertThat(emittedStates[1]).isEqualTo(mapOf(task1 to taskData1))
    collectJob.cancel()
  }

  @Test
  fun `getData returns correct data`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val taskData1 = createTaskData("data1")

    handler.setData(task1, taskData1)

    assertThat(handler.getData(task1)).isEqualTo(taskData1)
  }

  @Test
  fun `getData returns null for unknown task`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")

    assertThat(handler.getData(task1)).isNull()
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
    val task1 = createTask("task1")
    val taskData1 = createTaskData("data1")

    handler.setData(task1, taskData1)
    handler.setData(task1, null)

    val dataState = handler.dataState.first()
    assertThat(dataState).hasSize(1)
    assertThat(dataState[task1]).isNull()
  }

  private fun createTask(taskId: String): Task =
    FakeData.newTask(id = taskId, type = Task.Type.TEXT)

  private fun createTaskData(value: String): TextTaskData = TextTaskData(value)
}
