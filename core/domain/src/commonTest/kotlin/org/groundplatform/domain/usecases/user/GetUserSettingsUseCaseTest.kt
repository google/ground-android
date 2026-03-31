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
package org.groundplatform.domain.usecases.user

import kotlin.test.Test
import kotlin.test.assertEquals
import org.groundplatform.domain.helpers.FakeUserRepository
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.model.settings.UserSettings

class GetUserSettingsUseCaseTest {

  private val userRepository = FakeUserRepository()
  private val useCase = GetUserSettingsUseCase(userRepository)

  @Test
  fun `invoke returns user settings from repository`() {
    val expectedSettings =
      UserSettings(
        language = "en",
        measurementUnits = MeasurementUnits.IMPERIAL,
        shouldUploadPhotosOnWifiOnly = true,
      )
    userRepository.currentUserSettings = expectedSettings

    val result = useCase()

    assertEquals(expectedSettings, result)
  }
}
