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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveySyncMode
import org.groundplatform.domain.model.SurveySyncState
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface.SyncResult
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeLocationOfInterestRepository
import org.groundplatform.testing.FakeSurveyRepository

class SyncSurveyUseCaseTest {
  private val surveyRepository = FakeSurveyRepository()
  private val loiRepository = FakeLocationOfInterestRepository()
  private val syncSurvey = SyncSurveyUseCase(loiRepository, surveyRepository)

  @Test
  fun `Syncs survey and LOIs with remote`() = runTest {
    val survey = FakeDataGenerator.newSurvey()
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

    assertFailsWith<IllegalStateException> { syncSurvey(FakeDataGenerator.newSurvey().id) }
  }

  @Test
  fun `reads every LOI when the survey has never been synced`() = runTest {
    assertEquals(SurveySyncMode.Full, executeSync(syncState = null))
  }

  @Test
  fun `reads every LOI when the last sync covered a different survey data visibility setting`() =
    runTest {
      val state =
        SurveySyncState(
          surveyId = FakeDataGenerator.newSurvey().id,
          latestLoiServerTimestamp = TEST_LATEST_LOI_TIMESTAMP,
          lastFullSyncClientTimestamp = Clock.System.now().toEpochMilliseconds(),
          syncedDataVisibility = Survey.DataVisibility.ALL_SURVEY_PARTICIPANTS,
        )

      assertEquals(SurveySyncMode.Full, executeSync(state))
    }

  @Test
  fun `reads every LOI when the last full sync fell out of the message backlog`() = runTest {
    val state =
      SurveySyncState(
        surveyId = FakeDataGenerator.newSurvey().id,
        latestLoiServerTimestamp = TEST_LATEST_LOI_TIMESTAMP,
        lastFullSyncClientTimestamp =
          Clock.System.now().toEpochMilliseconds() - 29.days.inWholeMilliseconds,
        syncedDataVisibility = null,
      )

    assertEquals(SurveySyncMode.Full, executeSync(state))
  }

  @Test
  fun `resumes from the last cursor while the backlog still reaches it`() = runTest {
    val state =
      SurveySyncState(
        surveyId = FakeDataGenerator.newSurvey().id,
        latestLoiServerTimestamp = TEST_LATEST_LOI_TIMESTAMP,
        lastFullSyncClientTimestamp =
          Clock.System.now().toEpochMilliseconds() - 27.days.inWholeMilliseconds,
        syncedDataVisibility = null,
      )

    assertEquals(SurveySyncMode.Incremental(TEST_LATEST_LOI_TIMESTAMP), executeSync(state))
  }

  @Test
  fun `records where the sync of the LOIs left off`() = runTest {
    val survey = FakeDataGenerator.newSurvey()
    surveyRepository.remoteSurveys = listOf(survey)
    loiRepository.syncResult = SyncResult(SurveySyncMode.Full, TEST_LATEST_LOI_TIMESTAMP)

    syncSurvey(survey.id)

    assertEquals(
      SyncResult(SurveySyncMode.Full, TEST_LATEST_LOI_TIMESTAMP),
      surveyRepository.lastRecordedSyncState,
    )
  }

  private suspend fun executeSync(syncState: SurveySyncState?): SurveySyncMode? {
    val survey = FakeDataGenerator.newSurvey()
    surveyRepository.remoteSurveys = listOf(survey)
    surveyRepository.syncState = syncState

    syncSurvey(survey.id)

    return loiRepository.lastSyncMode
  }

  companion object {
    private const val TEST_LATEST_LOI_TIMESTAMP = 987654321L
  }
}
