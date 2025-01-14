/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.ui.datacollection

import com.google.android.ground.model.task.Task
import com.google.android.ground.model.task.Task.Type
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskSequenceHandlerTest {

  private val task1 = Task("task1", 1, Type.TEXT, "Task 1", true)
  private val task2 = Task("task2", 2, Type.TEXT, "Task 2", true)
  private val task3 = Task("task3", 3, Type.TEXT, "Task 3", true)
  private val task4 = Task("task4", 4, Type.TEXT, "Task 4", true)
  private val task5 = Task("task5", 5, Type.TEXT, "Task 5", true)

  private val allTasks = listOf(task1, task2, task3, task4, task5)
  private val taskDataHandler = TaskDataHandler()

  private fun createHandler(tasks: List<Task> = allTasks) =
    TaskSequenceHandler(tasks, taskDataHandler)

  @Test
  fun `constructor should throw error when tasks are empty`() {
    assertThrows(IllegalArgumentException::class.java) {
      TaskSequenceHandler(tasks = emptyList(), taskDataHandler = TaskDataHandler())
    }
  }

  @Test
  fun `generateTaskSequence returns all tasks when tasks don't contain any conditions`() {
    val handler = createHandler()
    val sequence = handler.generateTaskSequence()
    assertThat(sequence.toList()).isEqualTo(allTasks)
  }

  // Note: Add tests with task conditions and task selections

  @Test
  fun `isFirstPosition returns true for the first task`() {
    val handler = createHandler()
    assertThat(handler.isFirstPosition("task1")).isTrue()
  }

  @Test
  fun `isFirstPosition returns false for non-first tasks`() {
    val handler = createHandler()
    assertThat(handler.isFirstPosition("task2")).isFalse()
  }

  @Test
  fun `isFirstPosition returns false for task id missing from task sequence`() {
    val handler = createHandler()
    assertThat(handler.isFirstPosition("random")).isFalse()
  }

  @Test
  fun `isFirstPosition throws error for invalid task id`() {
    val handler = createHandler()
    assertThrows(IllegalArgumentException::class.java) { handler.isFirstPosition("") }
  }

  @Test
  fun `isLastPosition returns true for the last task`() {
    val handler = createHandler()
    assertThat(handler.isLastPosition("task5")).isTrue()
  }

  @Test
  fun `isLastPosition returns false for non-last tasks`() {
    val handler = createHandler()
    assertThat(handler.isLastPosition("task4")).isFalse()
  }

  @Test
  fun `isLastPosition returns false for task id missing from task sequence`() {
    val handler = createHandler()
    assertThat(handler.isLastPosition("random")).isFalse()
  }

  @Test
  fun `isLastPosition throws error for invalid task id`() {
    val handler = createHandler()
    assertThrows(IllegalArgumentException::class.java) { handler.isLastPosition("") }
  }

  @Test
  fun `getPreviousTask returns the previous task id`() {
    val handler = createHandler()
    assertThat(handler.getPreviousTask("task2")).isEqualTo("task1")
  }

  @Test
  fun `getPreviousTask throws error for invalid task id`() {
    val handler = createHandler()
    assertThrows(IllegalArgumentException::class.java) { handler.getPreviousTask("") }
  }

  @Test
  fun `getPreviousTask throws error when there is no previous task`() {
    val handler = createHandler()
    assertThrows(IllegalArgumentException::class.java) { handler.getPreviousTask("task1") }
  }

  @Test
  fun `getNextTask returns the next task id`() {
    val handler = createHandler()
    assertThat(handler.getNextTask("task2")).isEqualTo("task3")
  }

  @Test
  fun `getNextTask throws error for invalid task id`() {
    val handler = createHandler()
    assertThrows(IllegalArgumentException::class.java) { handler.getNextTask("") }
  }

  @Test
  fun `getNextTask throws error when there is no next task`() {
    val handler = createHandler()
    assertThrows(IndexOutOfBoundsException::class.java) { handler.getNextTask("task5") }
  }

  @Test
  fun `getAbsolutePosition returns the correct position`() {
    val handler = createHandler()
    assertThat(handler.getAbsolutePosition("task3")).isEqualTo(2)
  }

  @Test
  fun `getAbsolutePosition throws error for invalid task id`() {
    val handler = createHandler()
    assertThrows(IllegalArgumentException::class.java) { handler.getAbsolutePosition("") }
  }

  @Test
  fun `getAbsolutePosition throws error when task is not found`() {
    val handler = createHandler()
    assertThrows(IllegalArgumentException::class.java) { handler.getAbsolutePosition("invalid") }
  }

  // Note: Update test with task conditions fetching task index
  @Test
  fun `getTaskIndex returns the correct position`() {
    val handler = createHandler()
    assertThat(handler.getTaskIndex("task3")).isEqualTo(2)
  }

  @Test
  fun `getTaskIndex throws error for invalid task id`() {
    val handler = createHandler()
    assertThrows(IllegalArgumentException::class.java) { handler.getTaskIndex("") }
  }

  @Test
  fun `getTaskIndex throws error when task is not found`() {
    val handler = createHandler()
    assertThrows(IllegalArgumentException::class.java) { handler.getTaskIndex("invalid") }
  }

  // Note: Update test with task conditions fetching task position
  @Test
  fun `getTaskPosition returns the correct position`() {
    val handler = createHandler()
    val position = handler.getTaskPosition("task3")
    assertThat(position.absoluteIndex).isEqualTo(2)
    assertThat(position.relativeIndex).isEqualTo(2)
    assertThat(position.sequenceSize).isEqualTo(5)
  }
}
