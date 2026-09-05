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

package org.groundplatform.android.ui.offlineareas

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.OFFLINE_AREA
import org.groundplatform.android.data.local.stores.LocalOfflineAreaStore
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreasViewModelTest : BaseHiltTest() {

  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore
  @Inject lateinit var offlineAreasViewModel: OfflineAreasViewModel

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun `uiState initial value is loading and not empty`() {
    val initial = offlineAreasViewModel.uiState.value
    assertThat(initial.isLoading).isTrue()
    assertThat(initial.offlineAreas).isEmpty()
    assertThat(initial.isEmpty).isFalse()
  }

  @Test
  fun `uiState emits loaded areas from store with formatted size`() = runWithTestDispatcher {
    localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
    advanceUntilIdle()

    offlineAreasViewModel.uiState.test {
      val item = awaitItem()
      assertThat(item.isLoading).isFalse()
      assertThat(item.offlineAreas).hasSize(1)
      assertThat(item.offlineAreas[0].id).isEqualTo(OFFLINE_AREA.id)
      assertThat(item.offlineAreas[0].name).isEqualTo(OFFLINE_AREA.name)
      assertThat(item.offlineAreas[0].sizeOnDisk).isEqualTo("<1")
      assertThat(item.isEmpty).isFalse()
    }
  }

  @Test
  fun `uiState emits multiple areas and updates reactively`() = runWithTestDispatcher {
    val area1 = OFFLINE_AREA.copy(id = "id_1", name = "Area 1")
    val area2 = OFFLINE_AREA.copy(id = "id_2", name = "Area 2")

    localOfflineAreaStore.insertOrUpdate(area1)
    advanceUntilIdle()

    offlineAreasViewModel.uiState.test {
      val item1 = awaitItem()
      assertThat(item1.offlineAreas).hasSize(1)
      assertThat(item1.offlineAreas[0].name).isEqualTo("Area 1")

      localOfflineAreaStore.insertOrUpdate(area2)
      advanceUntilIdle()

      val item2 = awaitItem()
      assertThat(item2.offlineAreas).hasSize(2)
      assertThat(item2.offlineAreas.map { it.name }).containsExactly("Area 1", "Area 2")
    }
  }

  @Test
  fun `uiState emits empty state when no areas in store`() = runWithTestDispatcher {
    advanceUntilIdle()

    offlineAreasViewModel.uiState.test {
      val item = awaitItem()
      assertThat(item.isLoading).isFalse()
      assertThat(item.offlineAreas).isEmpty()
      assertThat(item.isEmpty).isTrue()
    }
  }

  @Test
  fun `showOfflineAreaSelector emits navigation event`() = runWithTestDispatcher {
    offlineAreasViewModel.navigateToOfflineAreaSelector.test {
      offlineAreasViewModel.showOfflineAreaSelector()
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(Unit)
    }
  }
}
