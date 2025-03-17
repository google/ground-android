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
package org.groundplatform.android.model.task

import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.groundplatform.android.model.submission.MultipleChoiceTaskData
import org.groundplatform.android.model.submission.TaskData
import org.junit.Test

typealias ExpressionTestCase = List<Pair<Boolean, Pair<String, TaskData>>>

typealias ConditionTestCase = List<Pair<Boolean, TaskSelections>>

const val TASK_A_ID = "task-A-id-123"
const val TASK_A_OPTION_X = "task-A-option-id-x"
const val TASK_A_OPTION_Y = "task-A-option-id-y"
const val TASK_A_OPTION_Z = "task-A-option-id-z"

const val TASK_B_ID = "task-B-id-123"
const val TASK_B_OPTION_X = "task-B-option-id-x"
const val TASK_B_OPTION_Y = "task-B-option-id-y"

val TASK_A_EXPRESSION =
  Expression(
    expressionType = Expression.ExpressionType.ANY_OF_SELECTED,
    taskId = TASK_A_ID,
    optionIds = setOf(TASK_A_OPTION_X),
  )
val Task_B_EXPRESSION =
  Expression(
    expressionType = Expression.ExpressionType.ANY_OF_SELECTED,
    taskId = TASK_B_ID,
    optionIds = setOf(TASK_B_OPTION_X),
  )

fun makeValue(vararg selectedOptions: String) =
  MultipleChoiceTaskData(multipleChoice = null, selectedOptionIds = selectedOptions.toList())

class ConditionTest {
  @Test
  fun `Condition of type MATCH_ANY works`() {
    val condition =
      Condition(
        matchType = Condition.MatchType.MATCH_ALL,
        listOf(TASK_A_EXPRESSION, Task_B_EXPRESSION),
      )
    listOf(
        // Expressions evaluate to [true, true].
        true to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_X), TASK_B_ID to makeValue(TASK_B_OPTION_X)),
        // Expressions evaluate to [true, false].
        false to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_X), TASK_B_ID to makeValue(TASK_B_OPTION_Y)),
        // Expressions evaluate to [false, true].
        false to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_Y), TASK_B_ID to makeValue(TASK_B_OPTION_X)),
        // Expressions evaluate to [false, false].
        false to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_Y), TASK_B_ID to makeValue(TASK_B_OPTION_Y)),
      )
      .test(condition)
  }

  @Test
  fun `Condition of type MATCH_ALL works`() {
    val condition =
      Condition(
        matchType = Condition.MatchType.MATCH_ALL,
        listOf(TASK_A_EXPRESSION, Task_B_EXPRESSION),
      )
    listOf(
        // Expressions evaluate to [true, true].
        true to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_X), TASK_B_ID to makeValue(TASK_B_OPTION_X)),
        // Expressions evaluate to [true, false].
        false to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_X), TASK_B_ID to makeValue(TASK_B_OPTION_Y)),
        // Expressions evaluate to [false, true].
        false to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_Y), TASK_B_ID to makeValue(TASK_B_OPTION_X)),
        // Expressions evaluate to [false, false].
        false to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_Y), TASK_B_ID to makeValue(TASK_B_OPTION_Y)),
      )
      .test(condition)
  }

  @Test
  fun `Condition of type MATCH_ONE works`() {
    val condition =
      Condition(
        matchType = Condition.MatchType.MATCH_ONE,
        listOf(TASK_A_EXPRESSION, Task_B_EXPRESSION),
      )
    listOf(
        // Expressions evaluate to [true, true].
        false to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_X), TASK_B_ID to makeValue(TASK_B_OPTION_X)),
        // Expressions evaluate to [true, false].
        true to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_X), TASK_B_ID to makeValue(TASK_B_OPTION_Y)),
        // Expressions evaluate to [false, true].
        true to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_Y), TASK_B_ID to makeValue(TASK_B_OPTION_X)),
        // Expressions evaluate to [false, false].
        false to
          mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_Y), TASK_B_ID to makeValue(TASK_B_OPTION_Y)),
      )
      .test(condition)
  }

  @Test
  fun `Condition of type UNKNOWN throws an error`() {
    val condition =
      Condition(
        matchType = Condition.MatchType.UNKNOWN,
        listOf(TASK_A_EXPRESSION, Task_B_EXPRESSION),
      )
    assertFailsWith<IllegalArgumentException> {
      condition.fulfilledBy(mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_X)))
    }
  }

  @Test
  fun `Expression of type ANY_OF_SELECTED works`() {
    val expression =
      Expression(
        expressionType = Expression.ExpressionType.ANY_OF_SELECTED,
        taskId = TASK_A_ID,
        optionIds = setOf(TASK_A_OPTION_X, TASK_A_OPTION_Y),
      )
    listOf(
        true to (TASK_A_ID to makeValue(TASK_A_OPTION_X)),
        true to (TASK_A_ID to makeValue(TASK_A_OPTION_Y)),
        true to (TASK_A_ID to makeValue(TASK_A_OPTION_X, TASK_A_OPTION_Y)),
        false to (TASK_A_ID to makeValue(TASK_A_OPTION_Z)),
        false to (TASK_B_ID to makeValue(TASK_A_OPTION_X)),
      )
      .test(expression)
  }

  @Test
  fun `Expression of type ALL_OF_SELECTED works`() {
    val expression =
      Expression(
        expressionType = Expression.ExpressionType.ALL_OF_SELECTED,
        taskId = TASK_A_ID,
        optionIds = setOf(TASK_A_OPTION_X, TASK_A_OPTION_Y),
      )
    listOf(
        false to (TASK_A_ID to makeValue(TASK_A_OPTION_X)),
        false to (TASK_A_ID to makeValue(TASK_A_OPTION_Y)),
        true to (TASK_A_ID to makeValue(TASK_A_OPTION_X, TASK_A_OPTION_Y)),
        false to (TASK_A_ID to makeValue(TASK_A_OPTION_Z)),
        false to (TASK_B_ID to makeValue(TASK_A_OPTION_X, TASK_A_OPTION_Y)),
      )
      .test(expression)
  }

  @Test
  fun `Expression of type ONE_OF_SELECTED works`() {
    val expression =
      Expression(
        expressionType = Expression.ExpressionType.ONE_OF_SELECTED,
        taskId = TASK_A_ID,
        optionIds = setOf(TASK_A_OPTION_X, TASK_A_OPTION_Y),
      )
    listOf(
        true to (TASK_A_ID to makeValue(TASK_A_OPTION_X)),
        true to (TASK_A_ID to makeValue(TASK_A_OPTION_Y)),
        false to (TASK_A_ID to makeValue(TASK_A_OPTION_X, TASK_A_OPTION_Y)),
        false to (TASK_A_ID to makeValue(TASK_A_OPTION_Z)),
        false to (TASK_B_ID to makeValue(TASK_A_OPTION_X)),
      )
      .test(expression)
  }

  @Test
  fun `Expression of type UNKNOWN throws an error`() {
    val expression =
      Expression(
        expressionType = Expression.ExpressionType.UNKNOWN,
        taskId = TASK_A_ID,
        optionIds = setOf(TASK_A_OPTION_X, TASK_A_OPTION_Y),
      )
    assertFailsWith<IllegalArgumentException> {
      expression.fulfilledBy(mapOf(TASK_A_ID to makeValue(TASK_A_OPTION_X)))
    }
  }

  private fun ConditionTestCase.test(condition: Condition) {
    this.forEachIndexed { index, testCase ->
      val message =
        "Type ${condition.matchType}, case $index: Fulfilled should be ${testCase.first} with ${testCase.second}"
      if (testCase.first) {
        assertTrue(condition.fulfilledBy(testCase.second), message)
      } else {
        assertFalse(condition.fulfilledBy(testCase.second), message)
      }
    }
  }

  private fun ExpressionTestCase.test(expression: Expression) {
    this.forEachIndexed { index, testCase ->
      val message =
        "Type ${expression.expressionType}, case $index: Fulfilled should be ${testCase.first} with ${testCase.second}"
      if (testCase.first) {
        assertTrue(expression.fulfilledBy(mapOf(testCase.second)), message)
      } else {
        assertFalse(expression.fulfilledBy(mapOf(testCase.second)), message)
      }
    }
  }
}
