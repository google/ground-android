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

package org.groundplatform.android.usecases.survey

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.proto.Survey.DataVisibility
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SyncSurveyUseCaseTest : BaseHiltTest() {
  @BindValue @Mock lateinit var loiRepository: LocationOfInterestRepository

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var syncSurvey: SyncSurveyUseCase

  @Test
  fun `Syncs survey and LOIs with remote`() = runBlocking {
    fakeRemoteDataStore.surveys = listOf(SURVEY)

    syncSurvey(SURVEY.id)

    assertThat(localSurveyStore.getSurveyById(SURVEY.id))
      .isEqualTo(SURVEY.copy(dataVisibility = null))
    verify(loiRepository).syncLocationsOfInterest(SURVEY)
  }

  @Test
  fun `when survey is not found in remote storage, should return null`() = runWithTestDispatcher {
    assertThat(syncSurvey("someUnknownSurveyId")).isNull()
    localSurveyStore.surveys.test { assertThat(expectMostRecentItem()).isEmpty() }
  }

  @Test
  fun `when remote survey load fails, should throw error`() {
    fakeRemoteDataStore.onLoadSurvey = { error("Something went wrong") }

    assertThrows(IllegalStateException::class.java) { runBlocking { syncSurvey(SURVEY.id) } }
  }

  @Test
  fun `should load all types of LOIs when visibility is ALL_SURVEY_PARTICIPANTS`() =
    runWithTestDispatcher {
      val survey = SURVEY.copy(dataVisibility = DataVisibility.ALL_SURVEY_PARTICIPANTS)
      fakeRemoteDataStore.surveys = listOf(survey)

      val predefinedLoi = LOCATION_OF_INTEREST.copy(id = "predefined_id")
      val userLoi = LOCATION_OF_INTEREST.copy(id = "user_id")
      val sharedLoi = LOCATION_OF_INTEREST.copy(id = "shared_id")

      fakeRemoteDataStore.predefinedLois = listOf(predefinedLoi)
      fakeRemoteDataStore.userLois = listOf(userLoi)
      fakeRemoteDataStore.sharedLois = listOf(sharedLoi)

      val expectedLois = setOf(predefinedLoi, userLoi, sharedLoi)
      whenever(loiRepository.getValidLois(survey))
        .thenReturn(kotlinx.coroutines.flow.flowOf(expectedLois))

      syncSurvey(survey.id)

      verify(loiRepository).syncLocationsOfInterest(survey)
      assertThat(loiRepository.getValidLois(survey).first()).isEqualTo(expectedLois)
    }
}
