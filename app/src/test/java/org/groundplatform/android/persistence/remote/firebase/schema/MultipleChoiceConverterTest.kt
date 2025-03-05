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

import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.persistence.remote.firebase.schema.MultipleChoiceConverter.toMultipleChoice
import org.groundplatform.android.proto.Task
import org.groundplatform.android.proto.TaskKt.MultipleChoiceQuestionKt.option
import org.groundplatform.android.proto.TaskKt.multipleChoiceQuestion
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class MultipleChoiceConverterTest {

  @Test
  fun `toMultipleChoice() converts from proto`() {
    val multipleChoiceProto = multipleChoiceQuestion {
      type = Task.MultipleChoiceQuestion.Type.SELECT_ONE
      hasOtherOption = true
      options.addAll(
        listOf(
          option {
            id = "id_1"
            label = "option 1"
          },
          option {
            id = "id_2"
            label = "option 2"
          },
        )
      )
    }
    assertThat(toMultipleChoice(multipleChoiceProto))
      .isEqualTo(
        MultipleChoice(
          options =
            persistentListOf(
              Option(id = "id_1", code = "id_1", label = "option 1"),
              Option(id = "id_2", code = "id_2", label = "option 2"),
            ),
          cardinality = MultipleChoice.Cardinality.SELECT_ONE,
          hasOtherOption = true,
        )
      )
  }
}
