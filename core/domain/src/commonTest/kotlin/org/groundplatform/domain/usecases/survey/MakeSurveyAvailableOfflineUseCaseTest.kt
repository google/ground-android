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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeLocationOfInterestRepository
import org.groundplatform.testing.FakeSurveyRepository

class MakeSurveyAvailableOfflineUseCaseTest {
  private val surveyRepository = FakeSurveyRepository()
  private val loiRepository = FakeLocationOfInterestRepository()
  private val syncSurveyUseCase = SyncSurveyUseCase(loiRepository, surveyRepository)
  private val makeSurveyAvailableOffline =
    MakeSurveyAvailableOfflineUseCase(surveyRepository, syncSurveyUseCase)

  @Test
  fun `when survey sync returns null, should return null`() = runTest {
    val result = makeSurveyAvailableOffline("non-existent-survey")

    assertNull(result)
    assertTrue(surveyRepository.subscribedSurveyIds.isEmpty())
  }

  @Test
  fun `when survey sync throws error, should throw error`() = runTest {
    surveyRepository.onGetRemoteSurveyCall.overrideBehavior { error("Remote failed") }

    assertFailsWith<IllegalStateException> { makeSurveyAvailableOffline("survey-1") }
    assertTrue(surveyRepository.subscribedSurveyIds.isEmpty())
  }

  @Test
  fun `when survey sync succeeds, should subscribe to updates`() = runTest {
    val survey = FakeDataGenerator.newSurvey(id = "survey-1")
    surveyRepository.remoteSurveys = listOf(survey)

    val result = makeSurveyAvailableOffline(survey.id)

    assertEquals(survey, result)
    assertTrue(surveyRepository.subscribedSurveyIds.contains(survey.id))
  }
}
