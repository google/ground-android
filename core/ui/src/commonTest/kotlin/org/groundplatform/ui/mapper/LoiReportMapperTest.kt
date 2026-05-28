/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.ui.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.ui.util.FakeDateFormatter
import org.groundplatform.ui.util.FakeStringResolver

class LoiReportMapperTest {

  private val mapper =
    LoiReportMapper(
      taskValueMapper =
        TaskValueMapper(strings = FakeStringResolver, dateFormatter = FakeDateFormatter),
      strings = FakeStringResolver,
      dateFormatter = FakeDateFormatter,
    )

  private val timestampSegment = "DATE0_TIME0"

  @Test
  fun `file name joins survey, loi, user, and timestamp with underscores`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "Loi",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(surveyName = "Survey", userName = "User"),
          ),
        submission = FakeDataGenerator.newSubmission(),
      )

    assertEquals("Survey_Loi_User_$timestampSegment", request!!.fileName)
  }

  @Test
  fun `file name strips characters outside the ASCII safe set`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "loi/name?",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(
                surveyName = "My Survey!",
                userName = "user@email.com",
              ),
          ),
        submission = FakeDataGenerator.newSubmission(),
      )

    assertEquals("MySurvey_loiname_useremailcom_$timestampSegment", request!!.fileName)
  }

  @Test
  fun `file name drops blank segments without leaving double underscores`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "loi",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(surveyName = "", userName = "@@@"),
          ),
        submission = FakeDataGenerator.newSubmission(),
      )

    assertEquals("loi_$timestampSegment", request!!.fileName)
  }

  @Test
  fun `file name strips non-ASCII characters`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "ß",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(surveyName = "café", userName = "José"),
          ),
        submission = FakeDataGenerator.newSubmission(),
      )

    // "café" -> "caf", "ß" -> "", "José" -> "Jos".
    assertEquals("caf_Jos_$timestampSegment", request!!.fileName)
  }

  @Test
  fun `file name is capped at 200 characters`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "x",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(surveyName = "a".repeat(300), userName = "y"),
          ),
        submission = FakeDataGenerator.newSubmission(),
      )

    assertEquals(200, request!!.fileName.length)
  }

  @Test
  fun `map returns null when submissionDetails are missing`() = runTest {
    val report = FakeDataGenerator.newLoiReport(submissionDetails = null)

    assertNull(mapper.map(report, FakeDataGenerator.newSubmission()))
  }
}
