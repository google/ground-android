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
package org.groundplatform.domain.usecases.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeOfflineAreaRepository
import org.groundplatform.testing.FakeSurveyRepository
import org.groundplatform.testing.FakeUserRepository

class ClearUserSessionUseCaseTest {
  private val offlineAreaRepository = FakeOfflineAreaRepository()
  private val surveyRepository = FakeSurveyRepository()
  private val userRepository = FakeUserRepository()
  private val survey = FakeDataGenerator.newSurvey()
  private val otherSurvey = FakeDataGenerator.newSurvey(id = "other survey id", jobMap = emptyMap())
  private val useCase =
    ClearUserSessionUseCase(offlineAreaRepository, surveyRepository, userRepository)

  @Test
  fun `Deletes all offline areas`() = runTest {
    offlineAreaRepository.downloadedAreas = listOf(FakeDataGenerator.newOfflineArea())

    useCase()

    assertEquals(emptyList(), offlineAreaRepository.downloadedAreas)
  }

  @Test
  fun `Clears active survey`() = runTest {
    surveyRepository.saveSurvey(survey)
    surveyRepository.activateSurvey(survey.id)

    useCase()

    assertNull(surveyRepository.activeSurvey)
    assertEquals("", surveyRepository.lastActiveSurveyId)
  }

  @Test
  fun `Unsubscribes from updates of all surveys on the device`() = runTest {
    surveyRepository.saveSurvey(survey)
    surveyRepository.saveSurvey(otherSurvey)
    surveyRepository.subscribeToSurveyUpdates(survey.id)
    surveyRepository.subscribeToSurveyUpdates(otherSurvey.id)

    useCase()

    assertTrue(surveyRepository.subscribedSurveyIds.isEmpty())
  }

  @Test
  fun `Unsubscribes from updates of the active survey`() = runTest {
    surveyRepository.saveSurvey(survey)
    surveyRepository.subscribeToSurveyUpdates(survey.id)
    surveyRepository.activateSurvey(survey.id)

    useCase()

    assertTrue(surveyRepository.subscribedSurveyIds.isEmpty())
  }

  @Test
  fun `Clears all user data`() = runTest {
    useCase()

    assertEquals(1, userRepository.clearUserDataCall.callCount)
  }

  @Test
  fun `Should not throw when nothing is stored locally`() = runTest {
    useCase()

    assertEquals(emptyList(), offlineAreaRepository.downloadedAreas)
    assertTrue(surveyRepository.subscribedSurveyIds.isEmpty())
    assertEquals(1, userRepository.clearUserDataCall.callCount)
  }
}
