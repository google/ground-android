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

package com.google.android.ground.persistence.remote.firebase.protobuf

import com.google.android.ground.TimberTestRule
import com.google.android.ground.persistence.remote.firebase.newDocumentSnapshot
import com.google.android.ground.proto.Survey
import com.google.android.ground.proto.Task.DateTimeQuestion.Type.BOTH_DATE_AND_TIME
import com.google.android.ground.proto.Task.MultipleChoiceQuestion.Type.SELECT_MULTIPLE
import com.google.android.ground.proto.TaskKt.dateTimeQuestion
import com.google.android.ground.proto.TaskKt.multipleChoiceQuestion
import com.google.android.ground.proto.survey
import com.google.android.ground.proto.task
import com.google.android.ground.test.deeplyNestedTestObject
import com.google.android.ground.test.nestedTestObject
import com.google.android.ground.test.testDocument
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import com.google.protobuf.GeneratedMessageLite
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FirestoreToProtobufExtTest(
  private val desc: String,
  private val input: DocumentSnapshot,
  private val idField: MessageFieldNumber?,
  private val expectedOutput: GeneratedMessageLite<*, *>,
) {
  @Test
  fun parseFrom() {
    val output = expectedOutput::class.parseFrom(input, idField)
    assertThat(output).isEqualTo(expectedOutput)
  }

  companion object {
    @get:ClassRule @JvmStatic var timberRule = TimberTestRule()

    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    @Suppress("LongMethod")
    fun data() =
      listOf(
        testCase(
          desc = "converts document id",
          id = "12345",
          idField = Survey.ID_FIELD_NUMBER,
          expected = survey { id = "12345" },
        ),
        testCase(
          desc = "ignores id when idField not specified",
          id = "12345",
          input = mapOf("2" to "n/a"),
          expected = survey { name = "n/a" },
        ),
        testCase(
          desc = "converts string fields",
          input = mapOf("2" to "something"),
          expected = survey { name = "something" },
        ),
        testCase(
          desc = "ignores non-numeric fields",
          input = mapOf("foo" to "bar"),
          expected = survey {},
        ),
        testCase(
          desc = "ignores unknown field numbers",
          input = mapOf("3000" to "bar"),
          expected = survey {},
        ),
        testCase(
          desc = "ignores bad type in string field",
          input = mapOf("2" to 123),
          expected = survey {},
        ),
        testCase(
          desc = "converts map<string, Message>",
          input = mapOf("2" to mapOf("key" to mapOf("1" to "foo"))),
          expected = testDocument { objMap["key"] = nestedTestObject { name = "foo" } },
        ),
        testCase(desc = "ignores bad type in map", input = mapOf("4" to 123), expected = survey {}),
        testCase(
          desc = "converts deep nested objects",
          input = mapOf("2" to mapOf("key" to mapOf("2" to mapOf("1" to "123")))),
          expected =
            testDocument {
              objMap["key"] = nestedTestObject {
                otherThing = deeplyNestedTestObject { id = "123" }
              }
            },
        ),
        testCase(
          desc = "ignores wrong type in map",
          input = mapOf("1" to "id123", "2" to mapOf("key" to "not a message!")),
          expected = testDocument { id = "id123" },
        ),
        testCase(
          desc = "ignores wrong type in deep nested object",
          input = mapOf("1" to "id234", "2" to mapOf("key" to mapOf("2" to "also not a message!"))),
          expected =
            testDocument {
              id = "id234"
              objMap["key"] = nestedTestObject {}
            },
        ),
        testCase(
          desc = "converts enum value",
          input = mapOf("1" to 3),
          expected = dateTimeQuestion { type = BOTH_DATE_AND_TIME },
        ),
        testCase(desc = "skips enum value 0", input = mapOf("3" to 0), expected = task {}),
        testCase(desc = "skips an unspecified enum value", input = mapOf(), expected = task {}),
        testCase(
          desc = "converts oneof messages",
          input = mapOf("10" to mapOf("1" to 2)),
          expected =
            task { multipleChoiceQuestion = multipleChoiceQuestion { type = SELECT_MULTIPLE } },
        ),
      )

    /** Help to improve readability by provided named args for positional test constructor args. */
    private fun testCase(
      desc: String,
      id: String = "",
      input: Map<String, Any> = mapOf(),
      idField: MessageFieldNumber? = null,
      expected: GeneratedMessageLite<*, *>,
    ) = arrayOf(desc, newDocumentSnapshot(id = id, data = input), idField, expected)
  }
}
