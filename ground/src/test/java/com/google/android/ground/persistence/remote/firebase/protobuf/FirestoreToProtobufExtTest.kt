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

import com.google.android.ground.persistence.remote.firebase.newDocumentSnapshot
import com.google.android.ground.proto.Survey
import com.google.android.ground.proto.job
import com.google.android.ground.proto.style
import com.google.android.ground.proto.survey
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import com.google.protobuf.GeneratedMessageLite
import com.sharedtest.TimberTestRule
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
          expected = survey { title = "n/a" },
        ),
        testCase(
          desc = "converts string fields",
          input = mapOf("2" to "something"),
          expected = survey { title = "something" },
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
          input = mapOf("4" to mapOf("job123" to mapOf("2" to "A job"))),
          expected = survey { jobs["job123"] = job { name = "A job" } },
        ),
        testCase(desc = "ignores bad type in map", input = mapOf("4" to 123), expected = survey {}),
        testCase(
          desc = "converts nested objects",
          input = mapOf("4" to mapOf("job123" to mapOf("3" to mapOf("1" to "#112233")))),
          expected = survey { jobs["job123"] = job { defaultStyle = style { color = "#112233" } } },
        ),
        testCase(
          desc = "ignores bad type for nested object",
          input = mapOf("2" to "test", "4" to mapOf("job123" to mapOf("3" to 123))),
          expected =
            survey {
              title = "test"
              jobs["job123"] = job {}
            },
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
