/*
 * Copyright 2026 Google LLC
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
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.groundplatform.domain.model.Survey
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeSurveyRepository

class GetSurveyListItemUseCaseTest {
  private val surveyRepository = FakeSurveyRepository()
  private val useCase = GetSurveyListItemUseCase(surveyRepository)

  @Test
  fun `Returns offline survey as SurveyListItem if survey is already in the device`() = runTest {
    val survey =
      FakeDataGenerator.newSurvey(
        id = "survey-1",
        title = "Offline Survey",
        description = "This is a test description",
        generalAccess = FakeDataGenerator.newGeneralAccess(Survey.GeneralAccess.UNLISTED),
      )
    surveyRepository.offlineSurveys = listOf(survey)

    val result = useCase(survey.id)

    assertEquals(survey.id, result?.id)
    assertEquals(survey.title, result?.title)
    assertEquals(survey.description, result?.description)
    assertEquals(survey.generalAccess, result?.generalAccess)
    assertEquals(result?.availableOffline, true)
  }

  @Test
  fun `Returns remote survey as SurveyListItem if it's not in the device yet`() = runTest {
    val survey =
      FakeDataGenerator.newSurvey(
        id = "survey-2",
        title = "Remote Survey",
        description = "This is a test description",
        generalAccess = FakeDataGenerator.newGeneralAccess(Survey.GeneralAccess.UNLISTED),
      )
    surveyRepository.remoteSurveys = listOf(survey)

    val result = useCase(survey.id)

    assertEquals(survey.id, result?.id)
    assertEquals(survey.title, result?.title)
    assertEquals(survey.description, result?.description)
    assertEquals(survey.generalAccess, result?.generalAccess)
    assertEquals(result?.availableOffline, false)
  }

  @Test
  fun `Returns null when survey not found in either offline or remote store`() = runTest {
    val result = useCase("unknown-survey")

    assertNull(result)
  }

  @Test
  fun `Prefers offline survey over remote survey when both exist`() = runTest {
    val offlineSurvey = FakeDataGenerator.newSurvey(id = "survey-3", title = "Offline Survey")
    val remoteSurvey = FakeDataGenerator.newSurvey(id = "survey-3", title = "Remote Survey")

    surveyRepository.offlineSurveys = listOf(offlineSurvey)
    surveyRepository.remoteSurveys = listOf(remoteSurvey)

    val result = useCase("survey-3")

    assertEquals("survey-3", result?.id)
    assertEquals("Offline Survey", result?.title)
    assertEquals(result?.availableOffline, true)
  }
}
