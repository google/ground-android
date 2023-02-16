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

import com.google.android.ground.BaseHiltTest
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.JOB
import com.sharedtest.FakeData.SURVEY
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.HiltAndroidTest
import java8.util.Optional
import javax.inject.Inject
import kotlin.test.assertFails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveyRepositoryTest : BaseHiltTest() {
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var surveyRepository: SurveyRepository
  @Inject lateinit var testDispatcher: TestDispatcher

  @Test
  fun activateSurvey_firstTime() =
    runTest(testDispatcher) {
      fakeRemoteDataStore.surveys = listOf(SURVEY)

      surveyRepository.activateSurvey(SURVEY.id)
      advanceUntilIdle()

      // Verify survey is available offline.
      surveyRepository.getOfflineSurvey(SURVEY.id).test().assertValue(SURVEY)
      // Verify survey is active.
      surveyRepository.activeSurvey.test().assertValues(Optional.of(SURVEY))
      // Verify app is subscribed to push updates.
      assertThat(fakeRemoteDataStore.isSubscribedToSurveyUpdates(SURVEY.id)).isTrue()
    }

  @Test
  fun activateSurvey_firstTime_handleRemoteFailure() =
    runTest(testDispatcher) {
      fakeRemoteDataStore.surveys = listOf()

      assertFails { surveyRepository.activateSurvey(SURVEY.id) }
      advanceUntilIdle()
      assertThat(fakeRemoteDataStore.isSubscribedToSurveyUpdates(SURVEY.id)).isFalse()
    }

  @Test
  fun activateSurvey_alreadyAvailableOffline() =
    runTest(testDispatcher) {
      fakeRemoteDataStore.surveys = listOf(SURVEY)
      surveyRepository.syncSurveyWithRemote(SURVEY.id).await()
      advanceUntilIdle()

      surveyRepository.activateSurvey(SURVEY.id)
      advanceUntilIdle()

      surveyRepository.activeSurvey.test().assertValue(Optional.of(SURVEY))
      assertThat(fakeRemoteDataStore.isSubscribedToSurveyUpdates(SURVEY.id)).isFalse()
    }

  @Test
  fun deleteSurvey_whenSurveyIsActive() =
    runTest(testDispatcher) {
      fakeRemoteDataStore.surveys = listOf(SURVEY)
      surveyRepository.syncSurveyWithRemote(SURVEY.id).await()
      advanceUntilIdle()
      surveyRepository.activateSurvey(SURVEY.id)
      advanceUntilIdle()

      surveyRepository.removeOfflineSurvey(SURVEY.id)
      advanceUntilIdle()

      // Verify survey is deleted
      surveyRepository.offlineSurveys.test().assertValues(listOf())
      // Verify survey deactivated
      assertThat(surveyRepository.activeSurveyId).isEmpty()
    }

  @Test
  fun deleteSurvey_whenSurveyIsInActive() =
    runTest(testDispatcher) {
      // Job ID must be globally unique.
      val job1 = JOB.copy(id = "job1")
      val job2 = JOB.copy(id = "job2")
      val survey1 = SURVEY.copy(id = "active survey id", jobMap = mapOf(job1.id to job1))
      val survey2 = SURVEY.copy(id = "inactive survey id", jobMap = mapOf(job2.id to job2))
      fakeRemoteDataStore.surveys = listOf(survey1, survey2)
      surveyRepository.syncSurveyWithRemote(survey1.id).await()
      surveyRepository.syncSurveyWithRemote(survey2.id).await()
      surveyRepository.activateSurvey(survey1.id)
      advanceUntilIdle()

      surveyRepository.removeOfflineSurvey(survey2.id)
      advanceUntilIdle()

      // Verify active survey isn't cleared
      assertThat(surveyRepository.activeSurveyId).isEqualTo(survey1.id)
      // Verify survey is deleted
      surveyRepository.offlineSurveys.test().assertValues(listOf(survey1))
    }
}
