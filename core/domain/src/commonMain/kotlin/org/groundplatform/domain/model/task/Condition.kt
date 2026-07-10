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
package org.groundplatform.domain.model.task

import org.groundplatform.domain.model.submission.MultipleChoiceTaskData
import org.groundplatform.domain.model.submission.TaskData

/** The task ID. */
typealias TaskId = String

/** The selected values keyed by task ID. */
typealias TaskSelections = Map<String, TaskData>

/**
 * Describes a user-defined condition on a task, which determines whether the given task should be
 * hidden due to failure of fulfillment based on the input expressions.
 */
data class Condition(
  /** Determines the evaluation condition for fulfillment (e.g. all or some expressions). */
  val matchType: MatchType = MatchType.UNKNOWN,
  /** The expressions to evaluate to fulfill the condition. */
  val expressions: List<Expression> = listOf(),
) {

  /** Match type names as they appear in the remote database. */
  enum class MatchType {
    UNKNOWN,
    MATCH_ANY,
    MATCH_ALL,
    MATCH_ONE,
  }

  /** Given the user's task selections, determine whether the condition is fulfilled. */
  fun fulfilledBy(taskSelections: TaskSelections) =
    when (matchType) {
      MatchType.MATCH_ANY -> expressions.any { it.fulfilledBy(taskSelections) }
      MatchType.MATCH_ALL -> expressions.all { it.fulfilledBy(taskSelections) }
      MatchType.MATCH_ONE -> expressions.count { it.fulfilledBy(taskSelections) } == 1
      MatchType.UNKNOWN -> throw IllegalArgumentException("Unknown match type: $matchType")
    }
}

data class Expression(
  /** Determines the evaluation condition for the expression (e.g. all or some selected options). */
  val expressionType: ExpressionType = ExpressionType.UNKNOWN,
  /** The task ID associated with this expression. */
  val taskId: String,
  /** The option IDs that need to be selected to fulfill the condition. */
  val optionIds: Set<String> = setOf(),
  /** Whether the 'Other' multiple choice option needs to be selected to fulfill the condition. */
  val otherSelected: Boolean = false,
) {

  /** Task type names as they appear in the remote database. */
  enum class ExpressionType {
    UNKNOWN,
    ANY_OF_SELECTED,
    ALL_OF_SELECTED,
    ONE_OF_SELECTED,
  }

  /** Given the selected options for this task, determine whether the expression is fulfilled. */
  fun fulfilledBy(taskSelections: TaskSelections): Boolean =
    taskSelections[this.taskId]?.let { selection -> this.fulfilled(selection) } ?: false

  private fun fulfilled(taskData: TaskData): Boolean {
    if (taskData !is MultipleChoiceTaskData) return false

    val selected = taskData.getSelectedOptionsIdsExceptOther().toMutableSet()
    if (taskData.isOtherTextSelected()) selected += MultipleChoiceTaskData.OTHER_ID

    val required = if (otherSelected) optionIds + MultipleChoiceTaskData.OTHER_ID else optionIds

    return when (expressionType) {
      ExpressionType.ANY_OF_SELECTED -> required.any { it in selected }
      ExpressionType.ALL_OF_SELECTED -> selected.containsAll(required)
      ExpressionType.ONE_OF_SELECTED -> selected.intersect(required).size == 1
      ExpressionType.UNKNOWN ->
        throw IllegalArgumentException("Unknown expression type: $expressionType")
    }
  }
}
