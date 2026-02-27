/*
 * Copyright 2026 Google LLC
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
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class UpdateUserSettingsUseCaseTest {

  @Mock lateinit var userRepository: UserRepository

  private lateinit var useCase: UpdateUserSettingsUseCase

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    useCase = UpdateUserSettingsUseCase(userRepository)
  }

  @Test
  fun `invoke updates user settings in repository`() {
    val settings =
      UserSettings(
        language = "en",
        measurementUnits = MeasurementUnits.METRIC,
        shouldUploadPhotosOnWifiOnly = true,
      )

    useCase(settings)

    verify(userRepository).setUserSettings(settings)
  }
}
