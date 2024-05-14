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

import com.google.android.ground.proto.Survey
import com.google.android.ground.proto.survey
import com.google.android.ground.test.deepNestedTestObject
import com.google.android.ground.test.nestedTestObject
import com.google.android.ground.test.testDocument
import com.google.common.truth.Truth
import com.sharedtest.TimberTestRule
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ProtobufToFirestoreExtTest(
  private val desc: String,
  private val input: Message,
  private val idField: MessageFieldNumber?,
  private val expectedOutput: Map<String, Any>,
) {
  @Test
  fun toFirestoreMap() {
    val output = input.toFirestoreMap(idField)
    Truth.assertThat(output).isEqualTo(expectedOutput)
  }

  companion object {
    @get:ClassRule @JvmStatic var timberRule = TimberTestRule()

    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() =
      listOf(
        testCase(
          desc = "ignores id field",
          input =
            survey {
              id = "123"
              name = "title"
            },
          idField = Survey.ID_FIELD_NUMBER,
          expected = mapOf("2" to "title"),
        ),
        testCase(
          desc = "converts string fields",
          input = survey { name = "something" },
          expected = mapOf("2" to "something"),
        ),
        testCase(
          desc = "converts map<string, Message>",
          input = testDocument { objMap["key"] = nestedTestObject { name = "foo" } },
          expected = mapOf("2" to mapOf("key" to mapOf("1" to "foo"))),
        ),
        testCase(
          desc = "converts deep nested objects",
          input =
            testDocument {
              objMap["key"] = nestedTestObject { otherThing = deepNestedTestObject { id = "123" } }
            },
          expected = mapOf("2" to mapOf("key" to mapOf("2" to mapOf("1" to "123")))),
        ),
      )

    /** Help to improve readability by provided named args for positional test constructor args. */
    private fun testCase(
      desc: String,
      input: Message,
      idField: MessageFieldNumber? = null,
      expected: Map<String, Any>,
    ) = arrayOf(desc, input, idField, expected)
  }
}
