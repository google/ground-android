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

package org.groundplatform.android.domain.usecase

import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.domain.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.android.domain.usecases.survey.ReactivateLastSurveyUseCase
import org.groundplatform.android.repository.SurveyRepository
import com.sharedtest.FakeData.SURVEY
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ReactivateLastSurveyUseCaseTest : BaseHiltTest() {
  @Inject lateinit var reactivateLastSurvey: ReactivateLastSurveyUseCase
  @Inject lateinit var surveyRepository: SurveyRepository

  @BindValue @Mock lateinit var activateSurvey: ActivateSurveyUseCase

  @Test
  fun `Reactivate last survey`() = runWithTestDispatcher {
    surveyRepository.lastActiveSurveyId = SURVEY.id
    reactivateLastSurvey()
    advanceUntilIdle()

    verify(activateSurvey).invoke(SURVEY.id)
  }

  @Test
  fun `Does nothing when a survey is already active`() = runWithTestDispatcher {
    activateSurvey(SURVEY.id)
    reactivateLastSurvey()
    advanceUntilIdle()

    // Tries to activate survey.
    verify(activateSurvey, times(1)).invoke(SURVEY.id)
  }

  @Test
  fun `Does nothing when survey never activated`() = runWithTestDispatcher {
    surveyRepository.lastActiveSurveyId = ""
    reactivateLastSurvey()
    advanceUntilIdle()

    // Should never try to activate survey.
    verify(activateSurvey, never()).invoke(SURVEY.id)
  }
}
