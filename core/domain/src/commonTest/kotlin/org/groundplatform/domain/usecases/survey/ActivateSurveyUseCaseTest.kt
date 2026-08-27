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

class ActivateSurveyUseCaseTest {
  private val surveyRepository = FakeSurveyRepository()
  private val loiRepository = FakeLocationOfInterestRepository()
  private val syncSurveyUseCase = SyncSurveyUseCase(loiRepository, surveyRepository)
  private val makeSurveyAvailableOffline =
    MakeSurveyAvailableOfflineUseCase(surveyRepository, syncSurveyUseCase)
  private val activateSurvey = ActivateSurveyUseCase(makeSurveyAvailableOffline, surveyRepository)

  @Test
  fun `Makes survey available offline and activates survey`() = runTest {
    val survey = FakeDataGenerator.newSurvey(id = "survey-1")
    surveyRepository.remoteSurveys = listOf(survey)

    val result = activateSurvey(survey.id)

    assertTrue(result)
    assertEquals(survey, surveyRepository.activeSurvey)
    assertEquals(survey, surveyRepository.getOfflineSurvey(survey.id))
  }

  @Test
  fun `Throws error when survey can't be made available offline`() = runTest {
    surveyRepository.onGetRemoteSurveyCall.overrideBehavior { error("Remote failed") }

    assertFailsWith<IllegalStateException> { activateSurvey("survey-1") }
    assertNull(surveyRepository.activeSurvey)
  }

  @Test
  fun `Throws error when survey doesn't exist`() = runTest {
    assertFailsWith<IllegalStateException> { activateSurvey("non-existent-survey") }
    assertNull(surveyRepository.activeSurvey)
  }

  @Test
  fun `Uses local instance if available`() = runTest {
    val survey = FakeDataGenerator.newSurvey(id = "survey-1")
    surveyRepository.offlineSurveys = listOf(survey)
    surveyRepository.onGetRemoteSurveyCall.overrideBehavior { error("Remote should not be called") }

    val result = activateSurvey(survey.id)

    assertTrue(result)
    assertEquals(survey, surveyRepository.activeSurvey)
  }

  @Test
  fun `Does nothing when survey already active`() = runTest {
    val survey = FakeDataGenerator.newSurvey(id = "survey-1")
    surveyRepository.offlineSurveys = listOf(survey)
    activateSurvey(survey.id)

    surveyRepository.onGetRemoteSurveyCall.overrideBehavior { error("Remote should not be called") }
    val result = activateSurvey(survey.id)

    assertTrue(result)
    assertEquals(survey, surveyRepository.activeSurvey)
  }
}
