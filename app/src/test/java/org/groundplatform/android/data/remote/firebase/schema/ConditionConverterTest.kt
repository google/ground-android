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

package org.groundplatform.android.data.remote.firebase.schema

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode
import kotlin.test.assertNull
import org.groundplatform.android.model.task.Condition
import org.groundplatform.android.model.task.Expression
import org.groundplatform.android.proto.TaskKt.condition
import org.groundplatform.android.proto.TaskKt.multipleChoiceSelection
import org.junit.Test

const val TASK_ID = "task-id-123"

class ConditionConverterTest {

  @Test
  fun `toCondition() converts return null for empty proto`() {
    with(ConditionConverter) { assertNull(condition {}.toCondition()) }
  }

  @OptIn(OnlyForUseByGeneratedProtoCode::class)
  @Test
  fun `toCondition() converts from proto`() {
    with(ConditionConverter) {
      val conditionProto = condition {
        multipleChoice = multipleChoiceSelection {
          taskId = TASK_ID
          optionIds.addAll(listOf("optionId1", "optionId2"))
        }
      }
      assertThat(conditionProto.toCondition())
        .isEqualTo(
          Condition(
            Condition.MatchType.MATCH_ANY,
            listOf(
              Expression(
                Expression.ExpressionType.ANY_OF_SELECTED,
                taskId = TASK_ID,
                setOf("optionId1", "optionId2"),
              )
            ),
          )
        )
    }
  }
}
