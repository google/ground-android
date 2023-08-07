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
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.domain.usecases.survey.MakeSurveyAvailableOfflineUseCase
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.repository.SurveyRepository
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.SURVEY
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ActivateSurveyUseCaseTest : BaseHiltTest() {
  @Inject lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var surveyRepository: SurveyRepository
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @BindValue @Mock lateinit var makeSurveyAvailableOffline: MakeSurveyAvailableOfflineUseCase

  @Test
  fun `Activates survey when no survey is active`() = runWithTestDispatcher {
    `when`(makeSurveyAvailableOffline(SURVEY.id)).thenReturn(SURVEY)

    activateSurvey(SURVEY.id)
    advanceUntilIdle()

    // Verify survey is made available for offline use.
    verify(makeSurveyAvailableOffline).invoke(SURVEY.id)
    // Verify survey is active.
    assertThat(surveyRepository.activeSurvey).isEqualTo(SURVEY)
  }

  @Test
  fun `Throws error when survey can't be made available offline`() = runWithTestDispatcher {
    `when`(makeSurveyAvailableOffline(SURVEY.id)).thenThrow(Error::class.java)

    assertFails { activateSurvey(SURVEY.id) }
    advanceUntilIdle()

    // Verify no survey is active.
    assertThat(surveyRepository.activeSurvey).isNull()
  }

  @Test
  fun `Throws error when survey doesn't exist`() = runWithTestDispatcher {
    `when`(makeSurveyAvailableOffline(SURVEY.id)).thenReturn(null)

    assertFails { activateSurvey(SURVEY.id) }
    advanceUntilIdle()

    // Verify no survey is active.
    assertThat(surveyRepository.activeSurvey).isNull()
  }

  @Test
  fun `Uses local instance if available`() = runWithTestDispatcher {
    fakeRemoteDataStore.surveys = listOf(SURVEY)
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    activateSurvey(SURVEY.id)
    advanceUntilIdle()

    // Verify that we don't try to make survey available offline again.
    verify(makeSurveyAvailableOffline, never()).invoke(SURVEY.id)
    // Verify survey is active.
    assertThat(surveyRepository.activeSurvey).isEqualTo(SURVEY)
  }

  @Test
  fun `Does nothing when survey already active`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    surveyRepository.activeSurvey = SURVEY

    activateSurvey(SURVEY.id)
    advanceUntilIdle()

    // Verify that we don't try to make survey available offline again.
    verify(makeSurveyAvailableOffline, never()).invoke(SURVEY.id)
    // Verify survey is active.
    assertThat(surveyRepository.activeSurvey).isEqualTo(SURVEY)
  }
}
