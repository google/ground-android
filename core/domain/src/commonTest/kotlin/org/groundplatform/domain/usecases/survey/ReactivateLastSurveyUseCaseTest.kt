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

package org.groundplatform.domain.usecases.survey

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeLocationOfInterestRepository
import org.groundplatform.testing.FakeSurveyRepository

class ReactivateLastSurveyUseCaseTest {
  private val surveyRepository = FakeSurveyRepository()
  private val loiRepository = FakeLocationOfInterestRepository()
  private val syncSurveyUseCase = SyncSurveyUseCase(loiRepository, surveyRepository)
  private val makeSurveyAvailableOffline =
    MakeSurveyAvailableOfflineUseCase(surveyRepository, syncSurveyUseCase)
  private val activateSurvey = ActivateSurveyUseCase(makeSurveyAvailableOffline, surveyRepository)
  private val reactivateLastSurvey = ReactivateLastSurveyUseCase(activateSurvey, surveyRepository)

  @Test
  fun `when last survey id is present, should activate it`() = runTest {
    val survey = FakeDataGenerator.newSurvey(id = "survey-1")
    surveyRepository.offlineSurveys = listOf(survey)
    surveyRepository.lastActiveSurveyId = survey.id

    val result = reactivateLastSurvey()

    assertTrue(result)
    assertEquals(survey, surveyRepository.activeSurvey)
  }

  @Test
  fun `when survey is already active, should do nothing`() = runTest {
    val survey = FakeDataGenerator.newSurvey(id = "survey-1")
    surveyRepository.offlineSurveys = listOf(survey)
    activateSurvey(survey.id)
    surveyRepository.lastActiveSurveyId = survey.id

    val result = reactivateLastSurvey()

    assertTrue(result)
    assertEquals(survey, surveyRepository.activeSurvey)
  }

  @Test
  fun `when last survey id is not present, should do nothing`() = runTest {
    surveyRepository.lastActiveSurveyId = ""

    val result = reactivateLastSurvey()

    assertFalse(result)
  }

  @Test
  fun `when last survey id is present but survey is not present, should throw exception`() =
    runTest {
      surveyRepository.lastActiveSurveyId = "non-existent-survey"

      assertFailsWith<IllegalStateException> { reactivateLastSurvey() }
    }
}
