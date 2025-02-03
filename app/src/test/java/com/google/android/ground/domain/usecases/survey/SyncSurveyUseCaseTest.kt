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

package com.google.android.ground.domain.usecases.survey

import app.cash.turbine.test
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.FakeData.SURVEY
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.remote.FakeRemoteDataStore
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
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

    assertThat(localSurveyStore.getSurveyById(SURVEY.id)).isEqualTo(SURVEY)
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
}
