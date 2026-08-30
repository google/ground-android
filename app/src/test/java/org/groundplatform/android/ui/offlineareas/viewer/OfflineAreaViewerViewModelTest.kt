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
package org.groundplatform.android.ui.offlineareas.viewer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.OFFLINE_AREA
import org.groundplatform.android.data.local.stores.LocalOfflineAreaStore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
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

    val state = offlineAreaViewerViewModel.uiState.value
    assertThat(state.area).isEqualTo(OFFLINE_AREA)
    assertThat(state.areaSize).isEqualTo("<1")
    assertThat(state.areaName).isEqualTo("Test Area")
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
  fun `progressOverlayVisible should be false by default and true when removing`() =
    runWithTestDispatcher {
      localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
      offlineAreaViewerViewModel.initialize("id_1")
      advanceUntilIdle()

      assertThat(offlineAreaViewerViewModel.uiState.value.isProgressOverlayVisible).isFalse()

      offlineAreaViewerViewModel.onRemoveButtonClick()

      assertThat(offlineAreaViewerViewModel.uiState.value.isProgressOverlayVisible).isTrue()
    }

  @Test
  fun `onRemoveButtonClick with null area does nothing`() = runWithTestDispatcher {
    assertThat(offlineAreaViewerViewModel.uiState.value.area).isNull()

    offlineAreaViewerViewModel.onRemoveButtonClick()

    assertThat(offlineAreaViewerViewModel.uiState.value.isProgressOverlayVisible).isFalse()
  }

  @Test
  fun `onRemoveButtonClick when isProgressOverlayVisible is already true does nothing`() =
    runWithTestDispatcher {
      localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
      offlineAreaViewerViewModel.initialize("id_1")
      advanceUntilIdle()

      // First click sets isProgressOverlayVisible to true
      offlineAreaViewerViewModel.onRemoveButtonClick()
      assertThat(offlineAreaViewerViewModel.uiState.value.isProgressOverlayVisible).isTrue()

      // Second click while in-progress should be ignored (debounced)
      offlineAreaViewerViewModel.onRemoveButtonClick()
      assertThat(offlineAreaViewerViewModel.uiState.value.isProgressOverlayVisible).isTrue()
    }

  @Test
  fun `onRemoveButtonClick successful removal emits navigateUp event and resets state`() =
    runWithTestDispatcher {
      localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
      offlineAreaViewerViewModel.initialize("id_1")
      advanceUntilIdle()

      offlineAreaViewerViewModel.navigateUp.test {
        offlineAreaViewerViewModel.onRemoveButtonClick()
        advanceUntilIdle()

        assertThat(awaitItem()).isEqualTo(Unit)
        assertThat(offlineAreaViewerViewModel.uiState.value.isProgressOverlayVisible).isFalse()
        assertThat(offlineAreaViewerViewModel.uiState.value.area).isNull()
      }
    }

  @Test
  fun `remove downloaded area failure resets progress overlay and does not navigate up`() =
    runWithTestDispatcher {
      val mockRepo =
        org.mockito.kotlin.mock<
          org.groundplatform.domain.repository.OfflineAreaRepositoryInterface
        >()
      org.mockito.kotlin.whenever(mockRepo.getOfflineArea("id_1")).thenReturn(OFFLINE_AREA)
      org.mockito.kotlin.whenever(mockRepo.sizeOnDevice(org.mockito.kotlin.any())).thenReturn(1024)
      org.mockito.kotlin
        .whenever(mockRepo.removeFromDevice(org.mockito.kotlin.any()))
        .thenThrow(RuntimeException("Deletion failed"))

      val testViewModel =
        OfflineAreaViewerViewModel(
          mockRepo,
          org.mockito.kotlin.mock(),
          org.mockito.kotlin.mock(),
          org.mockito.kotlin.mock(),
          org.mockito.kotlin.mock(),
          org.mockito.kotlin.mock(),
          org.mockito.kotlin.mock(),
          testDispatcher,
        )

      testViewModel.initialize("id_1")
      advanceUntilIdle()

      testViewModel.navigateUp.test {
        testViewModel.onRemoveButtonClick()
        advanceUntilIdle()

        assertThat(testViewModel.uiState.value.isProgressOverlayVisible).isFalse()
        expectNoEvents()
      }
    }

  @Test
  fun `initialize with non-existent area triggers navigateUp`() = runWithTestDispatcher {
    offlineAreaViewerViewModel.navigateUp.test {
      offlineAreaViewerViewModel.initialize("non_existent")
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(Unit)
    }
  }
}
