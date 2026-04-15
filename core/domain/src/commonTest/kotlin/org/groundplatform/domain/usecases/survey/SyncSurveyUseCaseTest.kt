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
import org.groundplatform.domain.helpers.FakeData
import org.groundplatform.domain.helpers.FakeLocationOfInterestRepository
import org.groundplatform.domain.helpers.FakeSurveyRepository

class SyncSurveyUseCaseTest {
  private val surveyRepository = FakeSurveyRepository()
  private val loiRepository = FakeLocationOfInterestRepository()
  private val syncSurvey = SyncSurveyUseCase(loiRepository, surveyRepository)

  @Test
  fun `Syncs survey and LOIs with remote`() = runTest {
    val survey = FakeData.newSurvey()
    surveyRepository.remoteSurveys = listOf(survey)

    syncSurvey(survey.id)

    assertEquals(1, surveyRepository.offlineSurveys.size)
    assertEquals(survey, surveyRepository.offlineSurveys.first())
    assertEquals(listOf(survey), loiRepository.syncLocationsOfInterestCall.calls)
  }

  @Test
  fun `when survey is not found in remote storage, should return null`() = runTest {
    val result = syncSurvey("someUnknownSurveyId")

    assertNull(result)
    assertTrue(surveyRepository.offlineSurveys.isEmpty())
    assertEquals(loiRepository.syncLocationsOfInterestCall.callCount, 0)
  }

  @Test
  fun `when remote survey load fails, should throw error`() = runTest {
    surveyRepository.onGetRemoteSurveyCall.overrideBehavior { error("Something went wrong") }

    assertFailsWith<IllegalStateException> { syncSurvey(FakeData.newSurvey().id) }
  }
}
