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
import com.google.android.ground.model.task.Condition.MatchType
import com.google.android.ground.model.task.Expression
import com.google.android.ground.model.task.Expression.ExpressionType
import com.google.android.ground.proto.Task
import timber.log.Timber

/** Converts between Firestore nested objects and [Condition] instances. */
internal object ConditionConverter {

  fun Task.Condition.toCondition(): Condition? {
    if (conditionTypeCase != Task.Condition.ConditionTypeCase.MULTIPLE_CHOICE) {
      Timber.e("Unsupported conditionType: $conditionTypeCase")
      return null
    }
    val expressions =
      listOf(
        Expression(
          ExpressionType.ANY_OF_SELECTED,
          taskId = multipleChoice.taskId,
          multipleChoice.optionIdsList.toSet(),
        )
      )
    return Condition(MatchType.MATCH_ANY, expressions)
  }

  fun ConditionNestedObject.toCondition(): Condition? {
    val matchType = matchType.toMatchType()
    if (matchType == MatchType.UNKNOWN) {
      Timber.e("Unsupported matchType: $matchType")
      return null
    }
    return Condition(matchType = matchType, expressions = (expressions ?: listOf()).toExpressions())
  }

  // Note: Key value must be in sync with web app.
  private fun String?.toMatchType(): MatchType =
    when (this) {
      "MATCH_ANY" -> {
        MatchType.MATCH_ANY
      }
      "MATCH_ALL" -> {
        MatchType.MATCH_ALL
      }
      "MATCH_ONE" -> {
        MatchType.MATCH_ONE
      }
      else -> {
        Timber.v("Unknown MatchType received: $this")
        MatchType.UNKNOWN
      }
    }

  private fun List<ExpressionNestedObject>.toExpressions(): List<Expression> =
    this.mapNotNull {
      val expressionType = it.expressionType.toExpressionType()
      if (expressionType == ExpressionType.UNKNOWN) {
        Timber.e("Unsupported expressionType: ${it.expressionType}, skipping expression.")
        null
      } else if (it.taskId == null) {
        Timber.e("Empty task ID encountered, skipping expression.")
        null
      } else if (it.optionIds == null) {
        Timber.e("Empty option IDs encountered, skipping expression.")
        null
      } else {
        Expression(
          expressionType = expressionType,
          taskId = it.taskId,
          optionIds = it.optionIds.toSet(),
        )
      }
    }

  private fun String?.toExpressionType(): ExpressionType =
    when (this) {
      "ANY_OF_SELECTED" -> {
        ExpressionType.ANY_OF_SELECTED
      }
      "ALL_OF_SELECTED" -> {
        ExpressionType.ALL_OF_SELECTED
      }
      "ONE_OF_SELECTED" -> {
        ExpressionType.ONE_OF_SELECTED
      }
      else -> {
        Timber.v("Unknown ExpressionType received: $this")
        ExpressionType.UNKNOWN
      }
    }
}
