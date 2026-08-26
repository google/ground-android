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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.map.CameraPosition
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeMapStateRepository
import org.groundplatform.testing.FakeSurveyRepository

class RemoveOfflineSurveyUseCaseTest {
  private val surveyRepository = FakeSurveyRepository()
  private val mapStateRepository = FakeMapStateRepository()
  private val useCase = RemoveOfflineSurveyUseCase(surveyRepository, mapStateRepository)

  private val survey = FakeDataGenerator.newSurvey()

  @Test
  fun `should delete local copy`() = runTest {
    surveyRepository.saveSurvey(survey)

    useCase(survey.id)

    assertEquals(emptyList(), surveyRepository.offlineSurveys)
  }

  @Test
  fun `should remove last camera position`() = runTest {
    surveyRepository.saveSurvey(survey)
    mapStateRepository.activeSurveyId = survey.id
    mapStateRepository.setCameraPosition(CameraPosition(Coordinates(0.0, 0.0)))

    useCase(survey.id)

    assertNull(mapStateRepository.getCameraPosition(survey.id))
  }

  @Test
  fun `should unsubscribe from survey updates`() = runTest {
    surveyRepository.saveSurvey(survey)
    surveyRepository.subscribeToSurveyUpdates(survey.id)

    useCase(survey.id)

    assertFalse(surveyRepository.subscribedSurveyIds.contains(survey.id))
  }

  @Test
  fun `should not unsubscribe from updates of other surveys`() = runTest {
    val otherSurvey = FakeDataGenerator.newSurvey(id = "other survey id", jobMap = emptyMap())
    surveyRepository.saveSurvey(survey)
    surveyRepository.saveSurvey(otherSurvey)
    surveyRepository.subscribeToSurveyUpdates(survey.id)
    surveyRepository.subscribeToSurveyUpdates(otherSurvey.id)

    useCase(survey.id)

    assertEquals(setOf(otherSurvey.id), surveyRepository.subscribedSurveyIds)
  }

  @Test
  fun `should not throw if local copy missing`() = runTest {
    useCase(survey.id)

    assertEquals(emptyList(), surveyRepository.offlineSurveys)
    assertTrue(surveyRepository.subscribedSurveyIds.isEmpty())
  }

  @Test
  fun `when active survey is same, should deactivate as well`() = runTest {
    surveyRepository.saveSurvey(survey)
    surveyRepository.activateSurvey(survey.id)

    useCase(survey.id)

    assertNull(surveyRepository.activeSurvey)
    assertEquals("", surveyRepository.lastActiveSurveyId)
  }

  @Test
  fun `when active survey is different, should not deactivate`() = runTest {
    val survey1 = FakeDataGenerator.newSurvey(id = "active survey id", jobMap = emptyMap())
    val survey2 = FakeDataGenerator.newSurvey(id = "inactive survey id", jobMap = emptyMap())
    surveyRepository.saveSurvey(survey1)
    surveyRepository.saveSurvey(survey2)
    surveyRepository.activateSurvey(survey1.id)

    useCase(survey2.id)

    // Verify that active survey isn't cleared or de-activated
    assertEquals(survey1, surveyRepository.activeSurvey)
    assertEquals(listOf(survey1), surveyRepository.offlineSurveys)
  }

  @Test
  fun `should not clear the camera position of other surveys`() = runTest {
    val otherSurvey = FakeDataGenerator.newSurvey(id = "other survey id", jobMap = emptyMap())
    val otherPosition = CameraPosition(Coordinates(1.0, 2.0))
    surveyRepository.saveSurvey(survey)
    surveyRepository.saveSurvey(otherSurvey)
    mapStateRepository.activeSurveyId = otherSurvey.id
    mapStateRepository.setCameraPosition(otherPosition)

    useCase(survey.id)

    assertEquals(otherPosition, mapStateRepository.getCameraPosition(otherSurvey.id))
  }
}
