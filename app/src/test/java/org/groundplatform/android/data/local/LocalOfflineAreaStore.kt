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
package org.groundplatform.android.data.local

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.data.local.stores.LocalOfflineAreaStore
import org.groundplatform.android.model.imagery.OfflineArea
import org.groundplatform.android.model.map.Bounds
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalOfflineAreaStore : BaseHiltTest() {

  @Inject lateinit var localOfflineStore: LocalOfflineAreaStore

  @Test
  fun `get offline areas`() = runWithTestDispatcher {
    localOfflineStore.insertOrUpdate(TEST_OFFLINE_AREA)
    localOfflineStore.offlineAreas().test {
      assertThat(expectMostRecentItem()).isEqualTo(listOf(TEST_OFFLINE_AREA))
    }
  }

  companion object {
    private val TEST_OFFLINE_AREA =
      OfflineArea("id_1", OfflineArea.State.PENDING, Bounds(0.0, 0.0, 0.0, 0.0), "Test Area", 0..14)
  }
}
