package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.task.Task
import com.sharedtest.FakeData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class TaskDataHandlerTest {

  @Test
  fun `setData updates dataState correctly`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val taskData1 = createTaskData("data1")

    handler.setData(task1, taskData1)

    val dataState = handler.dataState.first()
    assertEquals(1, dataState.size)
    assertEquals(taskData1, dataState[task1])
  }

  @Test
  fun `setData with same value does not update dataState`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val taskData1 = createTaskData("data1")

    handler.setData(task1, taskData1)
    val initialDataState = handler.dataState.first()

    handler.setData(task1, taskData1)
    val newDataState = handler.dataState.first()

    assertEquals(initialDataState, newDataState)
  }

  @Test
  fun `getData returns correct data`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val taskData1 = createTaskData("data1")

    handler.setData(task1, taskData1)

    assertEquals(taskData1, handler.getData(task1))
  }

  @Test
  fun `getData returns null for unknown task`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")

    assertNull(handler.getData(task1))
  }

  @Test
  fun `getTaskSelections returns correct selections`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val task2 = createTask("task2")
    val taskData1 = createTaskData("data1")
    val taskData2 = createTaskData("data2")

    handler.setData(task1, taskData1)
    handler.setData(task2, taskData2)

    val selections = handler.getTaskSelections()
    assertEquals(2, selections.size)
    assertEquals(taskData1, selections["task1"])
    assertEquals(taskData2, selections["task2"])
  }

  @Test
  fun `getTaskSelections with override returns correct selections`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val task2 = createTask("task2")
    val taskData1 = createTaskData("data1")
    val taskData2 = createTaskData("data2")
    val taskDataOverride = createTaskData("override")

    handler.setData(task1, taskData1)
    handler.setData(task2, taskData2)

    val selections = handler.getTaskSelections(Pair("task1", taskDataOverride))
    assertEquals(2, selections.size)
    assertEquals(taskDataOverride, selections["task1"])
    assertEquals(taskData2, selections["task2"])
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
    assertEquals(1, selections.size)
    assertEquals(taskData2, selections["task2"])
    assertNull(selections["task1"])
  }

  @Test
  fun `setData with null value`() = runTest {
    val handler = TaskDataHandler()
    val task1 = createTask("task1")
    val taskData1 = createTaskData("data1")

    handler.setData(task1, taskData1)
    handler.setData(task1, null)

    val dataState = handler.dataState.first()
    assertEquals(1, dataState.size)
    assertNull(dataState[task1])
  }

  private fun createTask(taskId: String): Task =
    FakeData.newTask(id = taskId, type = Task.Type.TEXT)

  private fun createTaskData(value: String): TextTaskData = TextTaskData(value)
}
