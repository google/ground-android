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

import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.persistence.remote.firebase.schema.MultipleChoiceConverter.toMultipleChoice
import com.google.android.ground.proto.Task
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertThrows
import org.junit.Test

class MultipleChoiceConverterTest {

  @Test
  fun `toMultipleChoice() converts from proto`() {
    val multipleChoiceProto =
      Task.MultipleChoiceQuestion.newBuilder()
        .setType(Task.MultipleChoiceQuestion.Type.SELECT_ONE)
        .setHasOtherOption(true)
        .addAllOptions(
          listOf(
            Task.MultipleChoiceQuestion.Option.newBuilder()
              .setId("id_1")
              .setLabel("option 1")
              .build(),
            Task.MultipleChoiceQuestion.Option.newBuilder()
              .setId("id_2")
              .setLabel("option 2")
              .build(),
          )
        )
        .build()

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

  @Test
  fun `toMultipleChoice() converts options when present`() {
    val taskNestedObject =
      TaskNestedObject(
        options =
          mapOf(
            Pair("id_1", OptionNestedObject(index = 0, code = "code_1", label = "option 1")),
            Pair("id_2", OptionNestedObject(index = 1, code = "code_2", label = "option 2")),
            Pair("id_3", OptionNestedObject(index = 2, code = "code_3", label = "option 3")),
          ),
        cardinality = "SELECT_ONE",
        hasOtherOption = false,
      )

    assertThat(toMultipleChoice(taskNestedObject))
      .isEqualTo(
        MultipleChoice(
          options =
            persistentListOf(
              Option(id = "id_1", code = "code_1", label = "option 1"),
              Option(id = "id_2", code = "code_2", label = "option 2"),
              Option(id = "id_3", code = "code_3", label = "option 3"),
            ),
          cardinality = MultipleChoice.Cardinality.SELECT_ONE,
          hasOtherOption = false,
        )
      )
  }

  @Test
  fun `toMultipleChoice() sorts options by index`() {
    val taskNestedObject =
      TaskNestedObject(
        options =
          mapOf(
            Pair("id_1", OptionNestedObject(index = 2, code = "code_1", label = "option 1")),
            Pair("id_2", OptionNestedObject(index = 0, code = "code_2", label = "option 2")),
            Pair("id_3", OptionNestedObject(index = 1, code = "code_3", label = "option 3")),
          ),
        cardinality = "SELECT_ONE",
        hasOtherOption = false,
      )

    assertThat(toMultipleChoice(taskNestedObject))
      .isEqualTo(
        MultipleChoice(
          options =
            persistentListOf(
              Option(id = "id_2", code = "code_2", label = "option 2"),
              Option(id = "id_3", code = "code_3", label = "option 3"),
              Option(id = "id_1", code = "code_1", label = "option 1"),
            ),
          cardinality = MultipleChoice.Cardinality.SELECT_ONE,
          hasOtherOption = false,
        )
      )
  }

  @Test
  fun `toMultipleChoice() defaults to empty list when no options`() {
    val taskNestedObject =
      TaskNestedObject(options = null, cardinality = "SELECT_ONE", hasOtherOption = false)

    assertThat(toMultipleChoice(taskNestedObject))
      .isEqualTo(
        MultipleChoice(
          options = persistentListOf(),
          cardinality = MultipleChoice.Cardinality.SELECT_ONE,
          hasOtherOption = false,
        )
      )
  }

  @Test
  fun `toMultipleChoice() when cardinality is SELECT_MULTIPLE`() {
    val taskNestedObject =
      TaskNestedObject(
        options =
          mapOf(
            Pair("id_1", OptionNestedObject(index = 0, code = "code_1", label = "option 1")),
            Pair("id_2", OptionNestedObject(index = 1, code = "code_2", label = "option 2")),
            Pair("id_3", OptionNestedObject(index = 2, code = "code_3", label = "option 3")),
          ),
        cardinality = "SELECT_MULTIPLE",
        hasOtherOption = false,
      )

    assertThat(toMultipleChoice(taskNestedObject))
      .isEqualTo(
        MultipleChoice(
          options =
            persistentListOf(
              Option(id = "id_1", code = "code_1", label = "option 1"),
              Option(id = "id_2", code = "code_2", label = "option 2"),
              Option(id = "id_3", code = "code_3", label = "option 3"),
            ),
          cardinality = MultipleChoice.Cardinality.SELECT_MULTIPLE,
          hasOtherOption = false,
        )
      )
  }

  @Test
  fun `toMultipleChoice() throws when cardinality is missing`() {
    val taskNestedObject =
      TaskNestedObject(options = null, cardinality = null, hasOtherOption = false)

    assertThrows(NullPointerException::class.java) { toMultipleChoice(taskNestedObject) }
  }

  @Test
  fun `toMultipleChoice() when hasOtherOption is true`() {
    val taskNestedObject =
      TaskNestedObject(
        options =
          mapOf(
            Pair("id_1", OptionNestedObject(index = 0, code = "code_1", label = "option 1")),
            Pair("id_2", OptionNestedObject(index = 1, code = "code_2", label = "option 2")),
            Pair("id_3", OptionNestedObject(index = 2, code = "code_3", label = "option 3")),
          ),
        cardinality = "SELECT_ONE",
        hasOtherOption = true,
      )

    assertThat(toMultipleChoice(taskNestedObject))
      .isEqualTo(
        MultipleChoice(
          options =
            persistentListOf(
              Option(id = "id_1", code = "code_1", label = "option 1"),
              Option(id = "id_2", code = "code_2", label = "option 2"),
              Option(id = "id_3", code = "code_3", label = "option 3"),
            ),
          cardinality = MultipleChoice.Cardinality.SELECT_ONE,
          hasOtherOption = true,
        )
      )
  }

  @Test
  fun `toMultipleChoice() default to false when hasOtherOption is missing`() {
    val taskNestedObject =
      TaskNestedObject(
        options =
          mapOf(
            Pair("id_1", OptionNestedObject(index = 0, code = "code_1", label = "option 1")),
            Pair("id_2", OptionNestedObject(index = 1, code = "code_2", label = "option 2")),
            Pair("id_3", OptionNestedObject(index = 2, code = "code_3", label = "option 3")),
          ),
        cardinality = "SELECT_ONE",
        hasOtherOption = null,
      )

    assertThat(toMultipleChoice(taskNestedObject))
      .isEqualTo(
        MultipleChoice(
          options =
            persistentListOf(
              Option(id = "id_1", code = "code_1", label = "option 1"),
              Option(id = "id_2", code = "code_2", label = "option 2"),
              Option(id = "id_3", code = "code_3", label = "option 3"),
            ),
          cardinality = MultipleChoice.Cardinality.SELECT_ONE,
          hasOtherOption = false,
        )
      )
  }
}
