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
import com.google.android.ground.domain.usecases.survey.MakeSurveyAvailableOfflineUseCase
import com.google.android.ground.repository.SurveyRepository
import com.sharedtest.FakeData.SURVEY
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertEquals
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
class MakeSurveyAvailableOfflineUseCaseTest : BaseHiltTest() {
  @Inject lateinit var makeSurveyAvailableOffline: MakeSurveyAvailableOfflineUseCase
  @BindValue @Mock lateinit var surveyRepository: SurveyRepository

  @Test
  fun `Returns null when survey doesn't exist`() = runWithTestDispatcher {
    `when`(surveyRepository.loadAndSyncSurveyWithRemote(SURVEY.id)).thenReturn(null)

    assertNull(makeSurveyAvailableOffline(SURVEY.id))
  }

  @Test
  fun `Throws error when survey can't be loaded`() {
    runBlocking {
      `when`(surveyRepository.loadAndSyncSurveyWithRemote(SURVEY.id)).thenThrow(Error::class.java)

      assertFails { makeSurveyAvailableOffline(SURVEY.id) }
    }
  }

  @Test
  fun `Returns survey on success`() = runWithTestDispatcher {
    `when`(surveyRepository.loadAndSyncSurveyWithRemote(SURVEY.id)).thenReturn(SURVEY)

    assertEquals(SURVEY, makeSurveyAvailableOffline(SURVEY.id))
  }

  @Test
  fun `Subscribes to updates on success`() = runWithTestDispatcher {
    `when`(surveyRepository.loadAndSyncSurveyWithRemote(SURVEY.id)).thenReturn(SURVEY)

    makeSurveyAvailableOffline(SURVEY.id)
    verify(surveyRepository).subscribeToSurveyUpdates(SURVEY.id)
  }
}
