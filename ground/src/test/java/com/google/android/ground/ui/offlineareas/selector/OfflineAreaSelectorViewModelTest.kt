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
package com.google.android.ground.ui.offlineareas.selector

import androidx.lifecycle.Observer
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.ui.map.Bounds
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorViewModelTest : BaseHiltTest() {

  @Inject lateinit var offlineAreaRepository: OfflineAreaRepository

  @Inject lateinit var offlineAreaSelectorViewModel: OfflineAreaSelectorViewModel

  @Test
  fun `downloadTiles handles exceptions correctly`() = runWithTestDispatcher {
    val exception = Exception("Test Exception")
    val boundsMock = mock(Bounds::class.java)

    whenever(offlineAreaRepository.downloadTiles(boundsMock)).thenReturn(flowOf(throw exception))

    val isFailureObserver = mock(Observer::class.java) as Observer<Boolean>
    val isDownloadProgressVisibleObserver = mock(Observer::class.java) as Observer<Boolean>

    offlineAreaSelectorViewModel.isFailure.observeForever(isFailureObserver)
    offlineAreaSelectorViewModel.isDownloadProgressVisible.observeForever(
      isDownloadProgressVisibleObserver
    )

    offlineAreaSelectorViewModel.onDownloadClick()

    advanceUntilIdle()

    verify(isFailureObserver).onChanged(true)
    verify(isDownloadProgressVisibleObserver).onChanged(false)

    offlineAreaSelectorViewModel.isFailure.removeObserver(isFailureObserver)
    offlineAreaSelectorViewModel.isDownloadProgressVisible.removeObserver(
      isDownloadProgressVisibleObserver
    )
  }
}
