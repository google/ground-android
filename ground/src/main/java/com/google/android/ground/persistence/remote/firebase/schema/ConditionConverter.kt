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
import timber.log.Timber

/** Converts between Firestore nested objects and [Condition] instances. */
internal object ConditionConverter {

  fun toCondition(em: ConditionNestedObject): Condition? {
    val matchType = toMatchType(em.matchType)
    if (matchType == Condition.MatchType.UNKNOWN) {
      Timber.e("Unsupported matchType: ${em.matchType}")
      return null
    }
    return Condition(
      matchType = matchType,
      expressions = toExpressions(em.expressions ?: listOf()),
    )
  }

  // Note: Key value must be in sync with web app.
  private fun toMatchType(typeStr: String?): Condition.MatchType =
    when (typeStr) {
      "MATCH_ANY" -> Condition.MatchType.MATCH_ANY
      "MATCH_ALL" -> Condition.MatchType.MATCH_ALL
      "MATCH_ONE" -> Condition.MatchType.MATCH_ONE
      else -> Condition.MatchType.UNKNOWN
    }.exhaustive

  private fun toExpressions(expressions: List<ExpressionNestedObject>): List<Expression> =
    expressions.mapNotNull {
      val expressionType = toExpressionType(it.expressionType)
      if (expressionType == Expression.ExpressionType.UNKNOWN) {
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

  private fun toExpressionType(typeStr: String?): Expression.ExpressionType =
    when (typeStr) {
      "ANY_OF_SELECTED" -> Expression.ExpressionType.ANY_OF_SELECTED
      "ALL_OF_SELECTED" -> Expression.ExpressionType.ALL_OF_SELECTED
      "ONE_OF_SELECTED" -> Expression.ExpressionType.ONE_OF_SELECTED
      else -> Expression.ExpressionType.UNKNOWN
    }.exhaustive

  private val <T> T.exhaustive: T
    get() = this
}
