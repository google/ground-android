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

package org.groundplatform.android.persistence.remote.firebase.schema

import org.groundplatform.android.model.task.Condition
import org.groundplatform.android.model.task.Condition.MatchType
import org.groundplatform.android.model.task.Expression
import org.groundplatform.android.model.task.Expression.ExpressionType
import org.groundplatform.android.proto.Task
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
}
