/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.repository

import app.cash.turbine.test
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.model.toListItem
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.JOB
import com.sharedtest.FakeData.SURVEY
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveyRepositoryTest : BaseHiltTest() {
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var surveyRepository: SurveyRepository
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var localSurveyStore: LocalSurveyStore

  @Before
  override fun setUp() {
    super.setUp()
    fakeRemoteDataStore.surveys = listOf(SURVEY)
  }

  @Test
  fun `setting selectedSurveyId updates the active survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    surveyRepository.selectedSurveyId = SURVEY.id
    advanceUntilIdle()

    surveyRepository.activeSurveyFlow.test { assertThat(expectMostRecentItem()).isEqualTo(SURVEY) }
    assertThat(surveyRepository.activeSurvey).isEqualTo(SURVEY)
  }

  @Test
  fun `clearActiveSurvey() resets active survey`() = runWithTestDispatcher {
    surveyRepository.clearActiveSurvey()
    advanceUntilIdle()

    surveyRepository.activeSurveyFlow.test { assertThat(expectMostRecentItem()).isNull() }
    assertThat(surveyRepository.activeSurvey).isNull()
  }

  @Test
  fun `removeOfflineSurvey() deletes local copy`() = runWithTestDispatcher {
    fakeRemoteDataStore.surveys = listOf(SURVEY)
    surveyRepository.loadAndSyncSurveyWithRemote(SURVEY.id)
    activateSurvey(SURVEY.id)

    surveyRepository.removeOfflineSurvey(SURVEY.id)
    advanceUntilIdle()

    surveyRepository.localSurveyListFlow.test { assertThat(expectMostRecentItem()).isEmpty() }
    assertThat(surveyRepository.activeSurvey).isNull()
  }

  @Test
  fun `removeOfflineSurvey() deactivates active survey`() = runWithTestDispatcher {
    fakeRemoteDataStore.surveys = listOf(SURVEY)
    surveyRepository.loadAndSyncSurveyWithRemote(SURVEY.id)
    activateSurvey(SURVEY.id)

    surveyRepository.removeOfflineSurvey(SURVEY.id)
    advanceUntilIdle()

    assertThat(surveyRepository.activeSurvey).isNull()
  }

  @Test
  fun `removeOfflineSurvey() removes inactive survey`() = runWithTestDispatcher {
    // Job ID must be globally unique.
    val job1 = JOB.copy(id = "job1")
    val job2 = JOB.copy(id = "job2")
    val survey1 = SURVEY.copy(id = "active survey id", jobMap = mapOf(job1.id to job1))
    val survey2 = SURVEY.copy(id = "inactive survey id", jobMap = mapOf(job2.id to job2))
    fakeRemoteDataStore.surveys = listOf(survey1, survey2)
    surveyRepository.loadAndSyncSurveyWithRemote(survey1.id)
    surveyRepository.loadAndSyncSurveyWithRemote(survey2.id)
    activateSurvey(survey1.id)
    advanceUntilIdle()

    surveyRepository.removeOfflineSurvey(survey2.id)
    advanceUntilIdle()

    // Verify active survey isn't cleared
    assertThat(surveyRepository.activeSurvey).isEqualTo(survey1)
    surveyRepository.localSurveyListFlow.test {
      assertThat(expectMostRecentItem()).isEqualTo(listOf(survey1.toListItem(true)))
    }
  }

  @Test
  fun `loadAndSyncSurveyWithRemote() returns null when survey not found`() = runWithTestDispatcher {
    assertNull(surveyRepository.loadAndSyncSurveyWithRemote("someUnknownSurveyId"))
  }

  @Test
  fun `loadAndSyncSurveyWithRemote() throws error when remote fetch fails`() =
    runWithTestDispatcher {
      fakeRemoteDataStore.onLoadSurvey = { error("Something went wrong") }
      assertFails { surveyRepository.loadAndSyncSurveyWithRemote("anySurveyId") }
    }
}
