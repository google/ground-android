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
import com.google.android.ground.coroutines.DefaultDispatcher
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.SURVEY
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.HiltAndroidTest
import java8.util.Optional
import javax.inject.Inject
import kotlin.test.assertFails
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.await
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
  @Inject lateinit var surveyStore: LocalSurveyStore
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var surveyRepository: SurveyRepository
  @DefaultDispatcher @Inject lateinit var testDispatcher: CoroutineDispatcher

  @Test
  fun activateSurvey_firstTime() =
    runTest(testDispatcher) {
      fakeRemoteDataStore.setTestSurvey(SURVEY)

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
      fakeRemoteDataStore.failOnLoadSurvey = true

      assertFails { surveyRepository.activateSurvey(SURVEY.id) }
      advanceUntilIdle()
      assertThat(fakeRemoteDataStore.isSubscribedToSurveyUpdates(SURVEY.id)).isFalse()
    }

  @Test
  fun activateSurvey_alreadyAvailableOffline() =
    runTest(testDispatcher) {
      surveyStore.insertOrUpdateSurvey(SURVEY).await()

      surveyRepository.activateSurvey(SURVEY.id)
      advanceUntilIdle()

      surveyRepository.activeSurvey.test().assertValue(Optional.of(SURVEY))
      assertThat(fakeRemoteDataStore.isSubscribedToSurveyUpdates(SURVEY.id)).isFalse()
    }
}
