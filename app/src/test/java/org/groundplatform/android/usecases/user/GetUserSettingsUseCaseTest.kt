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
package org.groundplatform.android.usecases.user

import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.repository.UserRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class GetUserSettingsUseCaseTest {

  @Mock lateinit var userRepository: UserRepository

  private lateinit var useCase: GetUserSettingsUseCase

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    useCase = GetUserSettingsUseCase(userRepository)
  }

  @Test
  fun `invoke returns user settings from repository`() {
    val expectedSettings =
      UserSettings(
        language = "en",
        measurementUnits = MeasurementUnits.METRIC,
        shouldUploadPhotosOnWifiOnly = false,
      )
    whenever(userRepository.getUserSettings()).thenReturn(expectedSettings)

    val result = useCase()

    assertEquals(expectedSettings, result)
    verify(userRepository).getUserSettings()
  }
}
