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
package org.groundplatform.domain.usecases

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.groundplatform.domain.model.AppConfig
import org.groundplatform.testing.FakeAppConfigRepository

class ShouldForceUpdateUseCaseTest {
  private val appConfigRepository = FakeAppConfigRepository()
  private val useCase = ShouldForceUpdateUseCase(appConfigRepository, currentVersion = "1.2.3")

  private fun setConfig(minAppVersion: String, forceUpdate: Boolean) {
    appConfigRepository.config = AppConfig(minAppVersion, forceUpdate)
  }

  @Test
  fun `Requires update when forced and app is older than the min version`() {
    setConfig(minAppVersion = "1.3.0", forceUpdate = true)

    assertTrue(useCase())
  }

  @Test
  fun `Does not require update when not forced`() {
    setConfig(minAppVersion = "1.3.0", forceUpdate = false)

    assertFalse(useCase())
  }

  @Test
  fun `Does not require update when app is at the min version`() {
    setConfig(minAppVersion = "1.2.3", forceUpdate = true)

    assertFalse(useCase())
  }

  @Test
  fun `Does not require update when app is newer than the min version`() {
    setConfig(minAppVersion = "1.2.0", forceUpdate = true)

    assertFalse(useCase())
  }

  @Test
  fun `Does not require update when min version is blank`() {
    setConfig(minAppVersion = "", forceUpdate = true)

    assertFalse(useCase())
  }

  @Test
  fun `Compares version segments numerically`() {
    setConfig(minAppVersion = "1.10.0", forceUpdate = true)

    assertTrue(useCase())
  }

  @Test
  fun `Reads the config again on each call`() {
    assertFalse(useCase())

    setConfig(minAppVersion = "1.3.0", forceUpdate = true)

    assertTrue(useCase())
  }
}
