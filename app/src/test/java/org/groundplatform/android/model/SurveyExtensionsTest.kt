/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.model

import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.proto.Survey
import org.junit.Test

class SurveyExtensionsTest {

  @Test
  fun `isUsable returns true when survey has predefined LOIs`() {
    val survey = createSurvey(emptyMap())

    // Test with predefined LOIs
    assertThat(survey.isUsable(loiCount = 5)).isTrue()
  }

  @Test
  fun `isUsable returns true when survey has ad hoc jobs`() {
    val jobWithAdHoc =
      Job(id = "job1", name = "Job 1", strategy = Job.DataCollectionStrategy.AD_HOC)

    val survey = createSurvey(mapOf("job1" to jobWithAdHoc))

    // Test with no LOIs but with ad hoc job
    assertThat(survey.isUsable(loiCount = 0)).isTrue()
  }

  @Test
  fun `isUsable returns true when survey has mixed jobs`() {
    val jobWithMixed = Job(id = "job1", name = "Job 1", strategy = Job.DataCollectionStrategy.MIXED)

    val survey = createSurvey(mapOf("job1" to jobWithMixed))

    // Test with no LOIs but with mixed job
    assertThat(survey.isUsable(loiCount = 0)).isTrue()
  }

  @Test
  fun `isUsable returns false when survey has no predefined LOIs and only predefined jobs`() {
    val jobWithPredefined =
      Job(id = "job1", name = "Job 1", strategy = Job.DataCollectionStrategy.PREDEFINED)

    val survey = createSurvey(mapOf("job1" to jobWithPredefined))

    // Test with no LOIs and only predefined job
    assertThat(survey.isUsable(loiCount = 0)).isFalse()
  }

  @Test
  fun `isUsable returns false when survey has no predefined LOIs and no jobs`() {
    val survey = createSurvey(emptyMap())

    // Test with no LOIs and no jobs
    assertThat(survey.isUsable(loiCount = 0)).isFalse()
  }

  private fun createSurvey(jobMap: Map<String, Job>) =
    Survey(
      id = "survey1",
      title = "Test Survey",
      description = "Test Description",
      jobMap = jobMap,
      generalAccess = Survey.GeneralAccess.RESTRICTED,
    )
}
