/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.usecases.session

import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.Test
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.data.local.room.LocalDatabase
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.repository.UserRepository
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ClearUserSessionUseCaseTest : BaseHiltTest() {

  @Mock lateinit var localDatabase: LocalDatabase
  @Mock lateinit var offlineAreaRepository: OfflineAreaRepository
  @Mock lateinit var surveyRepository: SurveyRepository
  @Mock lateinit var userRepository: UserRepository

  @InjectMocks lateinit var clearUserSessionUseCase: ClearUserSessionUseCase

  @Test
  fun `Deletes all offline areas`() = runWithTestDispatcher {
    clearUserSessionUseCase()
    verify(offlineAreaRepository, times(1)).removeAllOfflineAreas()
  }

  @Test
  fun `Clears active survey`() = runWithTestDispatcher {
    clearUserSessionUseCase()
    verify(surveyRepository, times(1)).clearActiveSurvey()
  }

  @Test
  fun `Clears user preference`() = runWithTestDispatcher {
    clearUserSessionUseCase()
    verify(userRepository, times(1)).clearUserPreferences()
  }

  @Test
  fun `Clears all tables`() = runWithTestDispatcher {
    clearUserSessionUseCase()
    verify(localDatabase, times(1)).clearAllTables()
  }
}
