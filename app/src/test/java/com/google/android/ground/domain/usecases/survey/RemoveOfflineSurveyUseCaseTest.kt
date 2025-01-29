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
package com.google.android.ground.domain.usecases.survey

import app.cash.turbine.test
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.FakeData.SURVEY
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.repository.SurveyRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class RemoveOfflineSurveyUseCaseTest : BaseHiltTest() {
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var removeOfflineSurveyUseCase: RemoveOfflineSurveyUseCase
  @Inject lateinit var surveyRepository: SurveyRepository

  @Test
  fun `should delete local copy`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    removeOfflineSurveyUseCase(SURVEY.id)

    localSurveyStore.surveys.test { assertThat(expectMostRecentItem()).isEmpty() }
  }

  @Test
  fun `should not throw if local copy missing`() = runWithTestDispatcher {
    removeOfflineSurveyUseCase(SURVEY.id)

    localSurveyStore.surveys.test { assertThat(expectMostRecentItem()).isEmpty() }
  }

  @Test
  fun `when active survey is same, should deactivate as well`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    activateSurvey(SURVEY.id)

    removeOfflineSurveyUseCase(SURVEY.id)

    assertThat(surveyRepository.activeSurvey).isNull()
  }

  @Test
  fun `when active survey is different, should not deactivate`() = runWithTestDispatcher {
    val survey1 = SURVEY.copy(id = "active survey id", jobMap = emptyMap())
    val survey2 = SURVEY.copy(id = "inactive survey id", jobMap = emptyMap())
    localSurveyStore.insertOrUpdateSurvey(survey1)
    localSurveyStore.insertOrUpdateSurvey(survey2)
    activateSurvey(survey1.id)
    advanceUntilIdle()

    removeOfflineSurveyUseCase(survey2.id)
    advanceUntilIdle()

    // Verify that active survey isn't cleared or de-activated
    assertThat(surveyRepository.activeSurvey).isEqualTo(survey1)
    localSurveyStore.surveys.test { assertThat(expectMostRecentItem()).isEqualTo(listOf(survey1)) }
  }
}
