/*
 * Copyright 2021 Google LLC
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
package org.groundplatform.android.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveySyncMode
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface.SyncResult
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.usecases.survey.ActivateSurveyUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveyRepositoryTest : BaseHiltTest() {
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var surveyRepository: SurveyRepositoryInterface

  @Before
  override fun setUp() {
    super.setUp()
    fakeRemoteDataStore.surveys = listOf(SURVEY)
  }

  @Test
  fun `getSyncState returns null for a survey which has never been synced`() =
    runWithTestDispatcher {
      localSurveyStore.insertOrUpdateSurvey(SURVEY)

      assertThat(surveyRepository.getSyncState(SURVEY.id)).isNull()
    }

  @Test
  fun `recordSyncState stores the timestamp and the visibility after a full read`() =
    runWithTestDispatcher {
      val survey = SURVEY.copy(dataVisibility = Survey.DataVisibility.ALL_SURVEY_PARTICIPANTS)
      localSurveyStore.insertOrUpdateSurvey(survey)

      surveyRepository.recordSyncState(
        survey,
        SyncResult(SurveySyncMode.Full, TEST_LATEST_LOI_TIMESTAMP),
      )

      val state = checkNotNull(surveyRepository.getSyncState(survey.id))
      assertThat(state.latestLoiServerTimestamp).isEqualTo(TEST_LATEST_LOI_TIMESTAMP)
      assertThat(state.syncedDataVisibility).isEqualTo(survey.dataVisibility)
      assertThat(state.lastFullSyncClientTimestamp).isGreaterThan(0)
    }

  @Test
  fun `recordSyncState updates only the timestamp after an incremental read`() =
    runWithTestDispatcher {
      val survey = SURVEY.copy(dataVisibility = Survey.DataVisibility.ALL_SURVEY_PARTICIPANTS)
      localSurveyStore.insertOrUpdateSurvey(survey)
      surveyRepository.recordSyncState(
        survey,
        SyncResult(SurveySyncMode.Full, TEST_LATEST_LOI_TIMESTAMP),
      )
      val afterFullRead = checkNotNull(surveyRepository.getSyncState(survey.id))

      surveyRepository.recordSyncState(
        survey,
        SyncResult(
          SurveySyncMode.Incremental(TEST_LATEST_LOI_TIMESTAMP),
          TEST_LATEST_LOI_TIMESTAMP + 1,
        ),
      )

      val state = checkNotNull(surveyRepository.getSyncState(survey.id))
      assertThat(state.latestLoiServerTimestamp).isEqualTo(TEST_LATEST_LOI_TIMESTAMP + 1)
      assertThat(state.lastFullSyncClientTimestamp)
        .isEqualTo(afterFullRead.lastFullSyncClientTimestamp)
      assertThat(state.syncedDataVisibility).isEqualTo(afterFullRead.syncedDataVisibility)
    }

  @Test
  fun `setting selectedSurveyId updates the active survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    surveyRepository.activateSurvey(SURVEY.id)
    advanceUntilIdle()

    surveyRepository.activeSurveyFlow.test { assertThat(expectMostRecentItem()).isEqualTo(SURVEY) }
    assertThat(surveyRepository.activeSurvey).isEqualTo(SURVEY)
  }

  @Test
  fun `clearActiveSurvey() resets active survey`() = runWithTestDispatcher {
    surveyRepository.clearActiveSurvey()
    advanceUntilIdle()

    surveyRepository.activeSurveyFlow.test { assertThat(expectMostRecentItem()).isNull() }
    assertThat(surveyRepository.activeSurvey).isNull()
  }

  @Test
  fun `subscribeToSurveyUpdates() subscribes in the remote data store`() = runWithTestDispatcher {
    surveyRepository.subscribeToSurveyUpdates(SURVEY.id)

    assertThat(fakeRemoteDataStore.subscribedSurveyIds.contains(SURVEY.id)).isTrue()
  }

  @Test
  fun `unsubscribeFromSurveyUpdates() unsubscribes in the remote data store`() =
    runWithTestDispatcher {
      surveyRepository.subscribeToSurveyUpdates(SURVEY.id)

      surveyRepository.unsubscribeFromSurveyUpdates(SURVEY.id)

      assertThat(fakeRemoteDataStore.subscribedSurveyIds.contains(SURVEY.id)).isFalse()
    }

  @Test
  fun `getRemoteSurvey throws error when loading remote survey times out`() =
    runWithTestDispatcher {
      fakeRemoteDataStore.onLoadSurvey = {
        kotlinx.coroutines.delay(35000)
        SURVEY
      }

      kotlin.test.assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
        surveyRepository.getRemoteSurvey(SURVEY.id)
      }
    }

  companion object {
    private const val TEST_LATEST_LOI_TIMESTAMP = 987654321L
  }
}
