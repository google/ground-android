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
package org.groundplatform.android.util

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SurveyDeepLinkParserTest {

  private val parser = SurveyDeepLinkParser("groundplatform.org", "/android/survey/")

  @Test
  fun `extracts survey id from raw string payload`() {
    assertThat(parser.parse("https://groundplatform.org/android/survey/testId"))
      .isEqualTo("testId")
  }

  @Test
  fun `tolerates surrounding whitespace`() {
    assertThat(parser.parse("  https://groundplatform.org/android/survey/testId  "))
      .isEqualTo("testId")
  }

  @Test
  fun `tolerates trailing slash`() {
    assertThat(parser.parse("https://groundplatform.org/android/survey/testId/"))
      .isEqualTo("testId")
  }

  @Test
  fun `ignores query string`() {
    assertThat(parser.parse("https://groundplatform.org/android/survey/testId?utm_source=email"))
      .isEqualTo("testId")
  }

  @Test
  fun `extracts survey id from Uri`() {
    val uri = Uri.parse("https://groundplatform.org/android/survey/testId")
    assertThat(parser.parse(uri)).isEqualTo("testId")
  }

  @Test
  fun `rejects http scheme`() {
    assertThat(parser.parse("http://groundplatform.org/android/survey/testId")).isNull()
  }

  @Test
  fun `rejects wrong host`() {
    assertThat(parser.parse("https://example.com/android/survey/testId")).isNull()
  }

  @Test
  fun `rejects wrong path`() {
    assertThat(parser.parse("https://groundplatform.org/web/survey/testId")).isNull()
  }

  @Test
  fun `rejects empty id`() {
    assertThat(parser.parse("https://groundplatform.org/android/survey/")).isNull()
  }

  @Test
  fun `rejects non-url payload`() {
    assertThat(parser.parse("just a plain string")).isNull()
  }

  @Test
  fun `host is configurable`() {
    val customParser = SurveyDeepLinkParser("staging.example.org", "/android/survey/")
    assertThat(customParser.parse("https://staging.example.org/android/survey/testId"))
      .isEqualTo("testId")
    assertThat(customParser.parse("https://groundplatform.org/android/survey/testId")).isNull()
  }

  @Test
  fun `path is configurable`() {
    val customParser = SurveyDeepLinkParser("groundplatform.org", "/ios/survey/")
    assertThat(customParser.parse("https://groundplatform.org/ios/survey/testId"))
      .isEqualTo("testId")
    assertThat(customParser.parse("https://groundplatform.org/android/survey/testId")).isNull()
  }
}