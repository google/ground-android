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
package org.groundplatform.android.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.groundplatform.domain.model.AppConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppConfigRepositoryTest {

  private val remoteConfig: FirebaseRemoteConfig = mock {
    on { getString("min_app_version") } doReturn "1.0.0"
    on { getBoolean("force_update") } doReturn false
  }
  private val repository = AppConfigRepository(remoteConfig)

  @Test
  fun `Returns the active config`() {
    assertThat(repository.getAppConfig())
      .isEqualTo(AppConfig(minAppVersion = "1.0.0", forceUpdate = false))
  }

  @Test
  fun `Returns newly activated values on the next call`() {
    repository.getAppConfig()
    whenever(remoteConfig.getString("min_app_version")).thenReturn("2.0.0")
    whenever(remoteConfig.getBoolean("force_update")).thenReturn(true)

    assertThat(repository.getAppConfig())
      .isEqualTo(AppConfig(minAppVersion = "2.0.0", forceUpdate = true))
  }
}
