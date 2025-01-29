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

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.FakeData.SURVEY
import com.google.android.ground.persistence.remote.FakeRemoteDataStore
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MakeSurveyAvailableOfflineUseCaseTest : BaseHiltTest() {
  @BindValue @Mock lateinit var syncSurveyUseCase: SyncSurveyUseCase

  @Inject lateinit var makeSurveyAvailableOffline: MakeSurveyAvailableOfflineUseCase
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @Test
  fun `when survey sync returns null, should return null`() = runWithTestDispatcher {
    whenever(syncSurveyUseCase(SURVEY.id)).thenReturn(null)

    val result = makeSurveyAvailableOffline(SURVEY.id)

    assertThat(result).isNull()
  }

  @Test
  fun `when survey sync throws error, should throw error`() = runWithTestDispatcher {
    whenever(syncSurveyUseCase(SURVEY.id)).thenThrow(Error::class.java)

    assertThrows(Error::class.java) { runBlocking { makeSurveyAvailableOffline(SURVEY.id) } }
  }

  @Test
  fun `when survey sync succeeds, should subscribe to updates`() = runWithTestDispatcher {
    whenever(syncSurveyUseCase(SURVEY.id)).thenReturn(SURVEY)

    val result = makeSurveyAvailableOffline(SURVEY.id)

    assertThat(result).isEqualTo(SURVEY)
    assertThat(fakeRemoteDataStore.isSubscribedToSurveyUpdates(SURVEY.id)).isTrue()
  }
}
