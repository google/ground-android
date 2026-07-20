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
package org.groundplatform.android.system.deeplink

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.data.local.LocalValueStore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayInstallReferrerServiceTest {

  private val localValueStore: LocalValueStore = mock()
  private val manager =
    PlayInstallReferrerService(ApplicationProvider.getApplicationContext(), localValueStore)

  @Test
  fun `parseSurveyId returns id when referrer holds only the survey id`() {
    assertThat(manager.parseSurveyId("survey_id=$SURVEY_ID")).isEqualTo(SURVEY_ID)
  }

  @Test
  fun `parseSurveyId returns id when referrer holds other params`() {
    assertThat(manager.parseSurveyId("source=ground&survey_id=$SURVEY_ID&medium=email"))
      .isEqualTo(SURVEY_ID)
  }

  @Test
  fun `parseSurveyId returns null for an organic referrer`() {
    assertThat(manager.parseSurveyId("source=google-play&medium=organic")).isNull()
  }

  @Test
  fun `parseSurveyId returns null for an empty referrer`() {
    assertThat(manager.parseSurveyId("")).isNull()
  }

  @Test
  fun `parseSurveyId returns null when the value is missing`() {
    assertThat(manager.parseSurveyId("survey_id=")).isNull()
  }

  @Test
  fun `parseSurveyId returns null when the value is blank`() {
    assertThat(manager.parseSurveyId("survey_id=%20")).isNull()
  }

  @Test
  fun `parseSurveyId ignores keys that merely end with the survey id key`() {
    assertThat(manager.parseSurveyId("other_survey_id=$SURVEY_ID")).isNull()
  }

  @Test
  fun `parseSurveyId returns null when a param has no value delimiter`() {
    assertThat(manager.parseSurveyId("survey_id")).isNull()
  }

  @Test
  fun `parseSurveyId ignores params with an empty key`() {
    assertThat(manager.parseSurveyId("=ignored&survey_id=$SURVEY_ID")).isEqualTo(SURVEY_ID)
  }

  @Test
  fun `getDeferredSurveyId returns null once the referrer has been consumed`() = runTest {
    whenever(localValueStore.isDeferredDeeplinkConsumed).thenReturn(true)

    assertThat(manager.getDeferredSurveyId()).isNull()
  }

  @Test
  fun `getDeferredSurveyId returns null when the referrer cannot be read`() = runTest {
    whenever(localValueStore.isDeferredDeeplinkConsumed).thenReturn(false)

    assertThat(manager.getDeferredSurveyId()).isNull()
  }

  companion object {
    private const val SURVEY_ID = "survey_123"
  }
}
