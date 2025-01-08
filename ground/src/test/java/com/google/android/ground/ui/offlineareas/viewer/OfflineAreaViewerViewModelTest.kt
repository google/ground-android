/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.ui.offlineareas.viewer

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.persistence.local.stores.LocalOfflineAreaStore
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData.OFFLINE_AREA
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaViewerViewModelTest : BaseHiltTest() {

  @Inject lateinit var offlineAreaViewerViewModel: OfflineAreaViewerViewModel
  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore

  @Test
  fun `viewModel initialized the required values`() = runWithTestDispatcher {
    localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)

    offlineAreaViewerViewModel.initialize("id_1")
    advanceUntilIdle()

    assertThat(offlineAreaViewerViewModel.area.value).isEqualTo(OFFLINE_AREA)
    assertThat(offlineAreaViewerViewModel.areaSize.value).isEqualTo("<1")
    assertThat(offlineAreaViewerViewModel.areaName.value).isEqualTo("Test Area")
  }

  @Test
  fun `remove downloaded area is successful`() = runWithTestDispatcher {
    localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
    offlineAreaViewerViewModel.initialize("id_1")
    advanceUntilIdle()

    offlineAreaViewerViewModel.onRemoveButtonClick()
    advanceUntilIdle()

    assertThat(localOfflineAreaStore.getOfflineAreaById("id_1")).isNull()
  }

  @Test
  fun `progressOverlayVisible should be null by default`() = runWithTestDispatcher {
    assertThat(offlineAreaViewerViewModel.progressOverlayVisible.value).isNull()

    offlineAreaViewerViewModel.onRemoveButtonClick()
    advanceUntilIdle()

    assertThat(offlineAreaViewerViewModel.progressOverlayVisible.value).isTrue()
  }
}
