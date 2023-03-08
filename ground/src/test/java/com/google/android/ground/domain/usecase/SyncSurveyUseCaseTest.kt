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
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SurveyRepository
import com.sharedtest.FakeData.SURVEY
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Single
import javax.inject.Inject
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
  @BindValue @Mock lateinit var surveyRepository: SurveyRepository
  @BindValue @Mock lateinit var loiRepository: LocationOfInterestRepository

  @Test
  fun syncsSurveyAndLois() = runBlocking {
    `when`(surveyRepository.syncSurveyWithRemote(SURVEY.id)).thenReturn(Single.just(SURVEY))

    syncSurvey(SURVEY.id)

    verify(surveyRepository).syncSurveyWithRemote(SURVEY.id)
    verify(loiRepository).syncLocationsOfInterest(SURVEY)
  }
}
