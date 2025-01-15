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

import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.task.Condition
import com.google.android.ground.model.task.Expression
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.model.task.Task.Type
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskSequenceHandlerTest {

  private val option1 = Option("option 1", "code 1", "label 1")
  private val conditionalOption = Option("option 2", "code 2", "label 2")
  private val multipleChoice =
    MultipleChoice(
      options = persistentListOf(option1, conditionalOption),
      cardinality = MultipleChoice.Cardinality.SELECT_ONE,
    )

  private val task1 = createTask("task1", 1)
  private val conditionalTask =
    createTask(
      "conditionalTask",
      2,
      Condition(
        matchType = Condition.MatchType.MATCH_ANY,
        expressions =
          listOf(
            Expression(
              expressionType = Expression.ExpressionType.ANY_OF_SELECTED,
              taskId = task1.id,
              optionIds = setOf(conditionalOption.id),
            )
          ),
      ),
    )
  private val task2 = createTask("task2", 3)

  private val allTasks = listOf(task1, conditionalTask, task2)

  private val taskDataHandler = TaskDataHandler()
  private val taskSequenceHandler = TaskSequenceHandler(allTasks, taskDataHandler)

  private fun createTask(taskId: String, index: Int, condition: Condition? = null) =
    Task(
      taskId,
      index,
      Type.MULTIPLE_CHOICE,
      label = "",
      true,
      multipleChoice = multipleChoice,
      condition = condition,
    )

  /** Iterates through each task and auto-fills all required values. */
  private fun satisfyAllConditions() {
    allTasks
      .mapNotNull { it.condition?.expressions?.firstOrNull() }
      .forEach { expression ->
        val taskId = expression.taskId
        val task = allTasks.first { it.id == taskId }
        val optionId = expression.optionIds.first()
        val newValue = MultipleChoiceTaskData(multipleChoice, listOf(optionId))
        taskDataHandler.setData(task, newValue)
      }
  }

  @Test
  fun `constructor should throw error when tasks are empty`() {
    assertThrows(IllegalArgumentException::class.java) {
      TaskSequenceHandler(tasks = emptyList(), taskDataHandler)
    }
  }

  @Test
  fun `getTaskSequence returns all tasks when condition is met`() {
    satisfyAllConditions()

    val sequence = taskSequenceHandler.getTaskSequence()

    assertThat(sequence.toList()).isEqualTo(allTasks)
  }

  @Test
  fun `getTaskSequence returns partial tasks when condition is not met`() {
    val sequence = taskSequenceHandler.getTaskSequence()

    assertThat(sequence.toList()).isEqualTo(listOf(task1, task2))
  }

  @Test
  fun `generateTaskSequence returns all tasks if conditions are satisfied`() {
    satisfyAllConditions()

    val sequence = taskSequenceHandler.generateTaskSequence()

    assertThat(sequence.toList()).isEqualTo(allTasks)
  }

  @Test
  fun `generateTaskSequence overrides conditions even if conditions are satisfied`() {
    satisfyAllConditions()

    val sequence =
      taskSequenceHandler.generateTaskSequence(
        taskSelections =
          mapOf(
            task1.id to
              MultipleChoiceTaskData(multipleChoice, selectedOptionIds = listOf(option1.id))
          )
      )

    assertThat(sequence.toList()).isEqualTo(listOf(task1, task2))
  }

  @Test
  fun `isFirstPosition returns true for the first task`() {
    assertThat(taskSequenceHandler.isFirstPosition(task1.id)).isTrue()
  }

  @Test
  fun `isFirstPosition returns false for non-first tasks`() {
    assertThat(taskSequenceHandler.isFirstPosition(task2.id)).isFalse()
  }

  @Test
  fun `isFirstPosition returns false for task id missing from task sequence`() {
    assertThat(taskSequenceHandler.isFirstPosition("random")).isFalse()
  }

  @Test
  fun `isFirstPosition throws error for invalid task id`() {
    assertThrows(IllegalArgumentException::class.java) { taskSequenceHandler.isFirstPosition("") }
  }

  @Test
  fun `isLastPosition returns true for the last task`() {
    assertThat(taskSequenceHandler.isLastPosition(task2.id)).isTrue()
  }

  @Test
  fun `isLastPosition returns false for non-last tasks`() {
    assertThat(taskSequenceHandler.isLastPosition(task1.id)).isFalse()
  }

  @Test
  fun `isLastPosition returns false for task id missing from task sequence`() {
    assertThat(taskSequenceHandler.isLastPosition("random")).isFalse()
  }

  @Test
  fun `isLastPosition throws error for invalid task id`() {
    assertThrows(IllegalArgumentException::class.java) { taskSequenceHandler.isLastPosition("") }
  }

  @Test
  fun `getPreviousTask returns the previous task id`() {
    assertThat(taskSequenceHandler.getPreviousTask(task2.id)).isEqualTo(task1.id)
  }

  @Test
  fun `getPreviousTask throws error for invalid task id`() {
    assertThrows(IllegalArgumentException::class.java) { taskSequenceHandler.getPreviousTask("") }
  }

  @Test
  fun `getPreviousTask throws error when there is no previous task`() {
    assertThrows(IllegalArgumentException::class.java) {
      taskSequenceHandler.getPreviousTask(task1.id)
    }
  }

  @Test
  fun `getNextTask returns the next task id`() {
    assertThat(taskSequenceHandler.getNextTask(task1.id)).isEqualTo(task2.id)
  }

  @Test
  fun `getNextTask throws error for invalid task id`() {
    assertThrows(IllegalArgumentException::class.java) { taskSequenceHandler.getNextTask("") }
  }

  @Test
  fun `getNextTask throws error when there is no next task`() {
    assertThrows(IllegalArgumentException::class.java) { taskSequenceHandler.getNextTask(task2.id) }
  }

  @Test
  fun `getAbsolutePosition returns the correct position`() {

    assertThat(taskSequenceHandler.getAbsolutePosition(task2.id)).isEqualTo(2)
  }

  @Test
  fun `getAbsolutePosition throws error for invalid task id`() {
    assertThrows(IllegalArgumentException::class.java) {
      taskSequenceHandler.getAbsolutePosition("")
    }
  }

  @Test
  fun `getAbsolutePosition throws error when task is not found`() {
    assertThrows(IllegalArgumentException::class.java) {
      taskSequenceHandler.getAbsolutePosition("invalid")
    }
  }

  @Test
  fun `getTaskIndex returns the correct position`() {
    satisfyAllConditions()
    assertThat(taskSequenceHandler.getTaskIndex(task2.id)).isEqualTo(2)
  }

  @Test
  fun `getTaskIndex throws error for invalid task id`() {
    assertThrows(IllegalArgumentException::class.java) { taskSequenceHandler.getTaskIndex("") }
  }

  @Test
  fun `getTaskIndex throws error when task is not found`() {
    assertThrows(IllegalArgumentException::class.java) {
      taskSequenceHandler.getTaskIndex("invalid")
    }
  }

  @Test
  fun `getTaskPosition returns the correct position`() {
    val position = taskSequenceHandler.getTaskPosition(task2.id)

    assertThat(position.absoluteIndex).isEqualTo(2)
    assertThat(position.relativeIndex).isEqualTo(1)
    assertThat(position.sequenceSize).isEqualTo(2)
  }

  @Test
  fun `getTaskPosition throws error if task not found`() {

    assertThrows(IllegalArgumentException::class.java) {
      taskSequenceHandler.getTaskPosition(conditionalTask.id)
    }
  }
}
