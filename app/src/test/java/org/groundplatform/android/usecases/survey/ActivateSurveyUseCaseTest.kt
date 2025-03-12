/*
 * Copyright 2023 Google LLC
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

package org.groundplatform.android.usecases.survey

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.remote.FakeRemoteDataStore
import org.groundplatform.android.repository.SurveyRepository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ActivateSurveyUseCaseTest : BaseHiltTest() {
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var surveyRepository: SurveyRepository
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @Before
  override fun setUp() {
    super.setUp()
    fakeRemoteDataStore.surveys = listOf(SURVEY)
  }

  @Test
  fun `Makes survey available offline`() = runWithTestDispatcher {
    activateSurvey(SURVEY.id)
    advanceUntilIdle()

    assertThat(localSurveyStore.getSurveyById(SURVEY.id)).isEqualTo(SURVEY)
  }

  @Test
  fun `Activates survey`() = runWithTestDispatcher {
    activateSurvey(SURVEY.id)
    advanceUntilIdle()

    assertThat(surveyRepository.activeSurvey).isEqualTo(SURVEY)
  }

  @Test
  fun `Throws error when survey can't be made available offline`() = runWithTestDispatcher {
    fakeRemoteDataStore.onLoadSurvey = { error("Boom!") }

    assertFails { activateSurvey(SURVEY.id) }
    advanceUntilIdle()

    assertThat(surveyRepository.activeSurvey).isNull()
  }

  @Test
  fun `Throws error when survey doesn't exist`() = runWithTestDispatcher {
    fakeRemoteDataStore.onLoadSurvey = { null }

    assertFails { activateSurvey(SURVEY.id) }
    advanceUntilIdle()

    assertThat(surveyRepository.activeSurvey).isNull()
  }

  @Test
  fun `Uses local instance if available`() = runWithTestDispatcher {
    fakeRemoteDataStore.onLoadSurvey = { error("This should not be called") }
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    activateSurvey(SURVEY.id)
    advanceUntilIdle()

    assertThat(surveyRepository.activeSurvey).isEqualTo(SURVEY)
  }

  @Test
  fun `Does nothing when survey already active`() = runWithTestDispatcher {
    activateSurvey(SURVEY.id)
    advanceUntilIdle()

    fakeRemoteDataStore.onLoadSurvey = { error("loadSurvey() should not be called here") }
    activateSurvey(SURVEY.id)
    advanceUntilIdle()
  }
}
