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
package org.groundplatform.feature.pdf.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.geometry.Polygon
import org.groundplatform.domain.model.job.Style
import org.groundplatform.domain.model.locationofinterest.AuditInfo
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.usecases.user.GetUserSettingsUseCase
import org.groundplatform.feature.pdf.helpers.FakeDateFormatter
import org.groundplatform.feature.pdf.helpers.FakeStringResolver
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeUserRepository

class LoiReportMapperTest {

  private val userRepository = FakeUserRepository()

  private val mapper =
    LoiReportMapper(
      taskValueMapper =
        TaskValueMapper(strings = FakeStringResolver, dateFormatter = FakeDateFormatter),
      strings = FakeStringResolver,
      dateFormatter = FakeDateFormatter,
      getUserSettings = GetUserSettingsUseCase(userRepository),
    )

  @Test
  fun `file name joins survey loi user and timestamp with underscores`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "Loi",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(surveyName = "Survey", userName = "User"),
          ),
        submission = submission,
      )

    assertEquals("Survey_Loi_User_$TEST_TIMESTAMP", request!!.fileName)
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
        submission = submission,
      )

    assertEquals("MySurvey_loiname_useremailcom_$TEST_TIMESTAMP", request!!.fileName)
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
        submission = submission,
      )

    assertEquals("loi_$TEST_TIMESTAMP", request!!.fileName)
  }

  @Test
  fun `file name preserves non-Latin characters`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "ເພີ່ມຈຸດສຳຫຼວດ",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(surveyName = "แบบสำรวจ", userName = "テスト"),
          ),
        submission = submission,
      )

    assertEquals("แบบสำรวจ_ເພີ່ມຈຸດສຳຫຼວດ_テスト_$TEST_TIMESTAMP", request!!.fileName)
  }

  @Test
  fun `file name sanitizes reserved characters and preserves accented Latin letters`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "ß",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(
                surveyName = "Café/São/José",
                userName = "Test?",
              ),
          ),
        submission = submission,
      )

    assertEquals("CaféSãoJosé_ß_Test_$TEST_TIMESTAMP", request!!.fileName)
  }

  @Test
  fun `file name is capped at 100 characters`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "x",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(surveyName = "a".repeat(300), userName = "y"),
          ),
        submission = submission,
      )

    assertEquals(100, request!!.fileName.length)
  }

  @Test
  fun `timestamp comes from the submission's own last modified date`() = runTest {
    val request =
      mapper.map(
        loiReport =
          FakeDataGenerator.newLoiReport(
            loiName = "Loi",
            submissionDetails =
              FakeDataGenerator.newSubmissionDetails(surveyName = "Survey", userName = "User"),
          ),
        submission =
          FakeDataGenerator.newSubmission(
            lastModified = AuditInfo(FakeDataGenerator.newUser(), clientTimestamp = 42L)
          ),
      )

    assertEquals("Survey_Loi_User_DATE42_TIME42", request!!.fileName)
  }

  @Test
  fun `mapper returns null when submissionDetails are missing`() = runTest {
    val report = FakeDataGenerator.newLoiReport(submissionDetails = null)

    assertNull(mapper.map(report, submission))
  }

  @Test
  fun `mapBlock contains the labels, geometry, style, and the area formatted in the user's metric units`() =
    runTest {
      val style = Style("#00FF00")
      val report =
        FakeDataGenerator.newLoiReport(
          submissionDetails =
            FakeDataGenerator.newSubmissionDetails(geometry = SQUARE_POLYGON, style = style)
        )
      userRepository.currentUserSettings =
        FakeDataGenerator.newUserSettings(measurementUnits = MeasurementUnits.METRIC)

      val mapBlock = mapper.map(report, submission)!!.document.mapBlock!!

      assertEquals(SQUARE_POLYGON, mapBlock.geometry)
      assertEquals(style, mapBlock.style)
      val area = mapBlock.area!!
      assertEquals("area", area.label)
      assertEquals("1.00 ha", area.value)
    }

  @Test
  fun `mapBlock area is formatted in the user's imperial units`() = runTest {
    val report =
      FakeDataGenerator.newLoiReport(
        submissionDetails = FakeDataGenerator.newSubmissionDetails(geometry = SQUARE_POLYGON)
      )
    userRepository.currentUserSettings =
      FakeDataGenerator.newUserSettings(measurementUnits = MeasurementUnits.IMPERIAL)

    val mapBlock = mapper.map(report, submission)!!.document.mapBlock!!

    assertEquals("2.48 ac", mapBlock.area!!.value)
  }

  @Test
  fun `mapBlock has no area when the geometry is not a polygon`() = runTest {
    val report =
      FakeDataGenerator.newLoiReport(
        submissionDetails =
          FakeDataGenerator.newSubmissionDetails(geometry = Point(Coordinates(0.0, 0.0)))
      )

    val mapBlock = mapper.map(report, submission)!!.document.mapBlock

    assertNull(mapBlock?.area)
  }

  private companion object {
    val SQUARE_POLYGON =
      Polygon(
        LinearRing(
          listOf(
            Coordinates(0.0, 0.0),
            Coordinates(0.0, 0.0009),
            Coordinates(0.0009, 0.0009),
            Coordinates(0.0009, 0.0),
            Coordinates(0.0, 0.0),
          )
        )
      )
    const val TEST_TIMESTAMP = "DATE0_TIME0"
    /** Submission with a fixed last-modified timestamp so date assertions are deterministic. */
    val submission =
      FakeDataGenerator.newSubmission(
        lastModified = AuditInfo(FakeDataGenerator.newUser(), clientTimestamp = 0L)
      )
  }
}
