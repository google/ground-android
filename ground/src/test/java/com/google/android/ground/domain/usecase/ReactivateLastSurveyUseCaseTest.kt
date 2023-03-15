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

package com.google.android.ground.domain.usecase

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.domain.usecases.survey.ReactivateLastSurveyUseCase
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.repository.SurveyRepository
import com.sharedtest.FakeData.SURVEY
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ReactivateLastSurveyUseCaseTest : BaseHiltTest() {
  @Inject lateinit var reactivateLastSurvey: ReactivateLastSurveyUseCase
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var surveyRepository: SurveyRepository
  @Inject lateinit var testDispatcher: TestDispatcher

  @BindValue @Mock lateinit var activateSurvey: ActivateSurveyUseCase

  @Test
  fun reactivateLastSurvey_lastIdSet_noActiveSurvey() =
    runTest(testDispatcher) {
      surveyRepository.lastActiveSurveyId = SURVEY.id

      reactivateLastSurvey()
      advanceUntilIdle()

      // Verify survey is made available for offline use.
      verify(activateSurvey).invoke(SURVEY.id)
    }

  @Test
  fun reactivateLastSurvey_lastIdNotSet() =
    runTest(testDispatcher) {
      reactivateLastSurvey()
      advanceUntilIdle()

      // Verify survey is made available for offline use.
      verify(activateSurvey, never()).invoke(SURVEY.id)
    }

  @Test
  fun reactivateLastSurvey_lastIdSet_activeSurvey() =
    runTest(testDispatcher) {
      surveyRepository.activeSurvey = SURVEY

      reactivateLastSurvey()
      advanceUntilIdle()

      // Verify survey is made available for offline use.
      verify(activateSurvey, never()).invoke(SURVEY.id)
    }
}
