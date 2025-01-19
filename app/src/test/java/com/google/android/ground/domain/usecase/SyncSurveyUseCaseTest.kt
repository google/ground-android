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

package com.google.android.ground.domain.usecase

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.domain.usecases.survey.SyncSurveyUseCase
import com.google.android.ground.repository.LocalSurveyRepository
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.RemoteSurveyRepository
import com.sharedtest.FakeData.SURVEY
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SyncSurveyUseCaseTest : BaseHiltTest() {
  @Inject lateinit var syncSurvey: SyncSurveyUseCase
  @BindValue @Mock lateinit var localSurveyRepository: LocalSurveyRepository
  @BindValue @Mock lateinit var loiRepository: LocationOfInterestRepository
  @BindValue @Mock lateinit var remoteSurveyRepository: RemoteSurveyRepository

  @Test
  fun `Syncs survey and LOIs with remote`() = runBlocking {
    `when`(remoteSurveyRepository.fetchSurvey(SURVEY.id)).thenReturn(SURVEY)

    syncSurvey(SURVEY.id)

    verify(localSurveyRepository).saveSurvey(SURVEY)
    verify(loiRepository).syncLocationsOfInterest(SURVEY)
  }

  @Test
  fun `Returns null when survey not found`() = runBlocking {
    `when`(remoteSurveyRepository.fetchSurvey(SURVEY.id)).thenReturn(null)

    assertNull(syncSurvey(SURVEY.id))
  }

  @Test
  fun `Throws error when load fails`() {
    runBlocking {
      `when`(remoteSurveyRepository.fetchSurvey(SURVEY.id)).thenThrow(Error::class.java)

      assertFails { syncSurvey(SURVEY.id) }
    }
  }
}
