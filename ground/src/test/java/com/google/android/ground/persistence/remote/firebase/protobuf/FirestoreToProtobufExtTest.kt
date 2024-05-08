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
import com.google.android.ground.proto.job
import com.google.android.ground.proto.style
import com.google.android.ground.proto.survey
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import com.google.protobuf.GeneratedMessageLite
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FirestoreToProtobufExtTest(
  private val desc: String,
  private val input: DocumentSnapshot,
  private val expectedOutput: GeneratedMessageLite<*, *>,
) {
  @Test
  fun toMessage() {
    val output = input.toMessage(expectedOutput.javaClass.kotlin)
    assertThat(output).isEqualTo(expectedOutput)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() =
      listOf(
        testCase(desc = "converts document id", id = "12345", expected = survey { id = "12345" }),
        testCase(
          desc = "converts string fields",
          data = mapOf("title" to "something"),
          expected = survey { title = "something" },
        ),
        testCase(
          desc = "ignores unknown fields",
          data = mapOf("foo" to "bar"),
          expected = survey {},
        ),
        testCase(
          desc = "ignores bad type in string field",
          data = mapOf("title" to 123),
          expected = survey {},
        ),
        testCase(
          desc = "converts map<string, Message> fields",
          data = mapOf("jobs" to mapOf("job123" to mapOf("name" to "A job"))),
          expected =
            survey {
              jobs["job123"] = job {
                id = "job123"
                name = "A job"
              }
            },
        ),
        testCase(
          desc = "ignores bad type in map",
          data = mapOf("jobs" to 123),
          expected = survey {},
        ),
        testCase(
          desc = "converts nested objects",
          data =
            mapOf(
              "jobs" to mapOf("job123" to mapOf("defaultStyle" to mapOf("color" to "#112233")))
            ),
          expected =
            survey {
              jobs["job123"] = job {
                id = "job123"
                defaultStyle = style { color = "#112233" }
              }
            },
        ),
        testCase(
          desc = "ignores bad type for nested object",
          data = mapOf("jobs" to mapOf("job123" to mapOf("defaultStyle" to 123))),
          expected =
            survey {
              jobs["job123"] = job {
                id = "job123"
                style {}
              }
            },
        ),
      )

    private fun testCase(
      desc: String,
      id: String = "",
      data: Map<String, Any> = mapOf(),
      expected: GeneratedMessageLite<*, *>,
    ) = arrayOf(desc, newDocumentSnapshot(id = id, data = data), expected)
  }
}
