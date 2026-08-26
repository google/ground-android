/*
 * Copyright 2025 Google LLC
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.groundplatform.domain.model.Survey
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeSurveyRepository

class GetDataSharingTermsUseCaseTest {
  private val surveyRepository = FakeSurveyRepository()
  private val getDataSharingTermsUseCase = GetDataSharingTermsUseCase(surveyRepository)

  private suspend fun activateSurvey(survey: Survey) {
    surveyRepository.offlineSurveys = listOf(survey)
    surveyRepository.activateSurvey(survey.id)
  }

  @Test
  fun `Fails with exception if no survey active`() {
    val result = getDataSharingTermsUseCase()

    assertTrue(result.isFailure)
    assertIs<IllegalStateException>(result.exceptionOrNull())
    assertEquals("No active survey", result.exceptionOrNull()?.message)
  }

  @Test
  fun `Fails with custom exception if custom data sharing terms are invalid`() = runTest {
    val survey = FakeDataGenerator.newSurvey(dataSharingTerms = Survey.DataSharingTerms.Custom(""))
    activateSurvey(survey)

    val result = getDataSharingTermsUseCase()

    assertTrue(result.isFailure)
    assertIs<GetDataSharingTermsUseCase.InvalidCustomSharingTermsException>(
      result.exceptionOrNull()
    )
  }

  @Test
  fun `Succeeds with null if data sharing terms is already accepted`() = runTest {
    val survey = FakeDataGenerator.newSurvey()
    activateSurvey(survey)
    surveyRepository.setDataSharingConsent(survey.id, true)

    val result = getDataSharingTermsUseCase()

    assertTrue(result.isSuccess)
    assertNull(result.getOrNull())
  }

  @Test
  fun `Succeeds with null if data sharing terms is missing`() = runTest {
    val survey = FakeDataGenerator.newSurvey().copy(dataSharingTerms = null)
    activateSurvey(survey)

    val result = getDataSharingTermsUseCase()

    assertTrue(result.isSuccess)
    assertNull(result.getOrNull())
  }

  @Test
  fun `Succeeds with data sharing terms if not already accepted`() = runTest {
    val survey = FakeDataGenerator.newSurvey()
    activateSurvey(survey)

    val result = getDataSharingTermsUseCase()

    assertTrue(result.isSuccess)
    assertEquals(survey.dataSharingTerms, result.getOrNull())
  }
}
