/*
 * Copyright 2020 Google LLC
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
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalSurveyStoreTest : BaseHiltTest() {
  @Inject lateinit var localSurveyStore: LocalSurveyStore

  @Test
  fun testInsertAndGetSurveys() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    assertThat(localSurveyStore.surveys.first()).containsExactly(SURVEY)
  }

  @Test
  fun testGetSurveyById() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    assertThat(localSurveyStore.getSurveyById(SURVEY.id)).isEqualTo(SURVEY)
  }

  @Test
  fun testDeleteSurvey() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    localSurveyStore.deleteSurvey(SURVEY)
    assertThat(localSurveyStore.surveys.first()).isEmpty()
  }

  @Test
  fun testRemovedJobFromSurvey() = runWithTestDispatcher {
    val job1 = Job("job 1", Style(""), "job 1 name")
    val job2 = Job("job 2", Style(""), "job 2 name")
    var survey =
      Survey("foo id", "foo survey", "foo survey description", mapOf(Pair(job1.id, job1)))
    localSurveyStore.insertOrUpdateSurvey(survey)
    survey = Survey("foo id", "foo survey", "foo survey description", mapOf(Pair(job2.id, job2)))
    localSurveyStore.insertOrUpdateSurvey(survey)
    val updatedSurvey = localSurveyStore.getSurveyById("foo id")
    assertThat(updatedSurvey?.jobs).hasSize(1)
    assertThat(updatedSurvey?.jobs?.first()).isEqualTo(job2)
  }
}
