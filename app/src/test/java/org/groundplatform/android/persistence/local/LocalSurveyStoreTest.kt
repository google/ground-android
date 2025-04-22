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
package org.groundplatform.android.persistence.local

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalSurveyStoreTest : BaseHiltTest() {
  @Inject lateinit var localSurveyStore: LocalSurveyStore

  @Test
  fun `insertOrUpdateSurvey() inserts new survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    assertThat(localSurveyStore.surveys.first()).containsExactly(SURVEY)
  }

  @Test
  fun `getSurveyById() retrieves survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    assertThat(localSurveyStore.getSurveyById(SURVEY.id)).isEqualTo(SURVEY)
  }

  @Test
  fun `deleteSurvey() removes survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    localSurveyStore.deleteSurvey(SURVEY)
    assertThat(localSurveyStore.surveys.first()).isEmpty()
  }

  @Test
  fun `insertOrUpdateSurvey() removes deleted jobs`() = runWithTestDispatcher {
    val job1 = Job("job 1", Style(""), "job 1 name")
    val job2 = Job("job 2", Style(""), "job 2 name")
    // Insert survey with two jobs.
    localSurveyStore.insertOrUpdateSurvey(
      SURVEY.copy(jobMap = mapOf(job1.id to job1, job2.id to job2))
    )

    // Update data survey, removing one job.
    localSurveyStore.insertOrUpdateSurvey(SURVEY.copy(jobMap = mapOf(job2.id to job2)))

    val updatedSurvey = localSurveyStore.getSurveyById(SURVEY.id)!!
    assertThat(updatedSurvey.jobs).isEqualTo(mapOf(job2.id to job2))
  }
}
