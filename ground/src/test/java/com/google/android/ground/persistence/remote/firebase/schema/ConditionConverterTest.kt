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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.task.Condition
import com.google.android.ground.model.task.Expression
import com.google.android.ground.persistence.remote.firebase.schema.ConditionConverter.toCondition
import com.google.common.truth.Truth.assertThat
import org.junit.Test

const val TASK_ID = "task-id-123"
const val TEST_MATCH_TYPE = "MATCH_ANY"
const val TEST_OPTION_ID_A = "option-id-a-123"
const val TEST_OPTION_ID_B = "option-id-b-123"
const val TEST_OPTION_ID_C = "option-id-c-123"
val TEST_OPTION_IDS = listOf(TEST_OPTION_ID_A, TEST_OPTION_ID_B, TEST_OPTION_ID_C)

class ConditionConverterTest {
  @Test
  fun `toCondition() converts match types`() {
    with(ConditionConverter) {
      listOf(
          "MATCH_ANY" to Condition.MatchType.MATCH_ANY,
          "MATCH_ALL" to Condition.MatchType.MATCH_ALL,
          "MATCH_ONE" to Condition.MatchType.MATCH_ONE,
        )
        .forEach {
          assertThat(ConditionNestedObject(matchType = it.first).toCondition()?.matchType)
            .isEqualTo(it.second)
        }
    }
  }

  @Test
  fun `toCondition() returns null for invalid match types`() {
    assertThat(ConditionNestedObject().toCondition()).isNull()
    assertThat(ConditionNestedObject(matchType = "MATCH_TWO").toCondition()).isNull()
    // Case sensitive.
    assertThat(ConditionNestedObject(matchType = "match_any").toCondition()).isNull()
  }

  @Test
  fun `toCondition() converts expressions`() {
    val expressions =
      ConditionNestedObject(
          matchType = TEST_MATCH_TYPE,
          expressions =
            listOf(
              ExpressionNestedObject(
                expressionType = "ANY_OF_SELECTED",
                taskId = TASK_ID,
                optionIds = TEST_OPTION_IDS,
              )
            ),
        )
        .toCondition()
        ?.expressions
    assertThat(expressions?.size).isEqualTo(1)
    assertThat(expressions?.get(0)).isNotNull()
    with(expressions?.get(0)!!) {
      assertThat(expressionType).isEqualTo(Expression.ExpressionType.ANY_OF_SELECTED)
      assertThat(taskId).isEqualTo(TASK_ID)
      assertThat(optionIds).isEqualTo(setOf(TEST_OPTION_ID_A, TEST_OPTION_ID_B, TEST_OPTION_ID_C))
    }
  }

  @Test
  fun `toCondition() converts expression types`() {
    val conditionObjectWithExpressionType = { expressionType: String ->
      ConditionNestedObject(
        matchType = TEST_MATCH_TYPE,
        expressions =
          listOf(
            ExpressionNestedObject(
              expressionType = expressionType,
              taskId = TASK_ID,
              optionIds = TEST_OPTION_IDS,
            )
          ),
      )
    }
    listOf(
        "ANY_OF_SELECTED" to Expression.ExpressionType.ANY_OF_SELECTED,
        "ALL_OF_SELECTED" to Expression.ExpressionType.ALL_OF_SELECTED,
        "ONE_OF_SELECTED" to Expression.ExpressionType.ONE_OF_SELECTED,
      )
      .forEach {
        val condition = conditionObjectWithExpressionType(it.first).toCondition()
        val expressionType = condition?.expressions?.get(0)?.expressionType
        assertThat(expressionType).isEqualTo(it.second)
      }
  }

  @Test
  fun `toCondition() filters out invalid expression types`() {
    assertThat(
        ConditionNestedObject(
            matchType = TEST_MATCH_TYPE,
            expressions =
              listOf(
                ExpressionNestedObject(expressionType = null, taskId = TASK_ID),
                ExpressionNestedObject(expressionType = "TWO_OF_SELECTED", taskId = TASK_ID),
                // Case sensitive.
                ExpressionNestedObject(expressionType = "any_of_selected", taskId = TASK_ID),
              ),
          )
          .toCondition()
          ?.expressions
      )
      .isEqualTo(listOf<String>())
  }

  @Test
  fun `toCondition() filters out expressions with no task ID`() {
    assertThat(
        ConditionNestedObject(
            matchType = TEST_MATCH_TYPE,
            expressions =
              listOf(
                ExpressionNestedObject(
                  expressionType = "ANY_OF_SELECTED",
                  taskId = TASK_ID,
                  optionIds = TEST_OPTION_IDS,
                ),
                // Missing task ID.
                ExpressionNestedObject(
                  expressionType = "ANY_OF_SELECTED",
                  optionIds = TEST_OPTION_IDS,
                ),
              ),
          )
          .toCondition()
          ?.expressions
          ?.size
      )
      .isEqualTo(1)
  }

  @Test
  fun `toCondition() filters out expressions with no option IDs`() {
    assertThat(
        ConditionNestedObject(
            matchType = TEST_MATCH_TYPE,
            expressions =
              listOf(
                ExpressionNestedObject(
                  expressionType = "ANY_OF_SELECTED",
                  taskId = TASK_ID,
                  optionIds = TEST_OPTION_IDS,
                ),
                // Missing option IDs.
                ExpressionNestedObject(expressionType = "ANY_OF_SELECTED", taskId = TASK_ID),
              ),
          )
          .toCondition()
          ?.expressions
          ?.size
      )
      .isEqualTo(1)
  }
}
