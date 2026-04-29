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
package org.groundplatform.domain.usecases.survey

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseSurveyQrCodeUseCaseTest {

  private val parseSurveyQrCode = ParseSurveyQrCodeUseCase("groundplatform.org", "/android/survey/")

  @Test
  fun `extracts survey id from canonical https url`() {
    assertEquals(
      "testId",
      parseSurveyQrCode("https://groundplatform.org/android/survey/testId"),
    )
  }

  @Test
  fun `accepts http scheme`() {
    assertEquals("testId", parseSurveyQrCode("http://groundplatform.org/android/survey/testId"))
  }

  @Test
  fun `tolerates surrounding whitespace`() {
    assertEquals(
      "testId",
      parseSurveyQrCode("  https://groundplatform.org/android/survey/testId  "),
    )
  }

  @Test
  fun `tolerates trailing slash`() {
    assertEquals("testId", parseSurveyQrCode("https://groundplatform.org/android/survey/testId/"))
  }

  @Test
  fun `rejects wrong host`() {
    assertNull(parseSurveyQrCode("https://example.com/android/survey/testId"))
  }

  @Test
  fun `rejects wrong path`() {
    assertNull(parseSurveyQrCode("https://groundplatform.org/web/survey/testId"))
  }

  @Test
  fun `rejects empty id`() {
    assertNull(parseSurveyQrCode("https://groundplatform.org/android/survey/"))
  }

  @Test
  fun `rejects non-url payload`() {
    assertNull(parseSurveyQrCode("just a plain string"))
  }

  @Test
  fun `rejects id with disallowed characters`() {
    assertNull(parseSurveyQrCode("https://groundplatform.org/android/survey/abc 123"))
  }

  @Test
  fun `host is configurable`() {
    val parser = ParseSurveyQrCodeUseCase("staging.example.org", "/android/survey/")
    assertEquals("testId", parser("https://staging.example.org/android/survey/testId"))
    assertNull(parser("https://groundplatform.org/android/survey/testId"))
  }

  @Test
  fun `path is configurable`() {
    val parser = ParseSurveyQrCodeUseCase("groundplatform.org", "/web/survey/")
    assertEquals("testId", parser("https://groundplatform.org/web/survey/testId"))
    assertNull(parser("https://groundplatform.org/android/survey/testId"))
  }
}
