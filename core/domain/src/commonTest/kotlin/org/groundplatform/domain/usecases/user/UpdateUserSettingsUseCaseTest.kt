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
package org.groundplatform.domain.usecases.user

import kotlin.test.Test
import kotlin.test.assertEquals
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.model.settings.UserSettings
import org.groundplatform.testcommon.FakeUserRepository

class UpdateUserSettingsUseCaseTest {

  private val userRepository = FakeUserRepository()
  private val useCase = UpdateUserSettingsUseCase(userRepository)

  @Test
  fun `invoke updates user settings in repository`() {
    val settings =
      UserSettings(
        language = "en",
        measurementUnits = MeasurementUnits.IMPERIAL,
        shouldUploadPhotosOnWifiOnly = true,
      )

    useCase(settings)

    assertEquals(settings, userRepository.getUserSettings())
  }
}
