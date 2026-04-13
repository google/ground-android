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
package org.groundplatform.android.ui.offlineareas.selector

import app.cash.turbine.test
import dagger.hilt.android.testing.HiltAndroidTest
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.ui.offlineareas.selector.model.OfflineAreaSelectorEvent
import org.groundplatform.android.ui.offlineareas.selector.model.OfflineAreaSelectorState
import org.groundplatform.android.util.toMbString
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorViewModelTest : BaseHiltTest() {

  @Mock private lateinit var offlineAreaRepository: OfflineAreaRepository
  @Mock private lateinit var locationManager: LocationManager
  @Mock private lateinit var surveyRepository: SurveyRepository
  @Mock private lateinit var mapStateRepository: MapStateRepository
  @Mock private lateinit var settingsManager: SettingsManager
  @Mock private lateinit var permissionsManager: PermissionsManager
  @Mock private lateinit var locationOfInterestRepository: LocationOfInterestRepositoryInterface
  @Mock private lateinit var networkManager: NetworkManager

  private lateinit var viewModel: OfflineAreaSelectorViewModel

  @Before
  override fun setUp() {
    super.setUp()
    viewModel =
      OfflineAreaSelectorViewModel(
        offlineAreaRepository,
        testDispatcher,
        locationManager,
        surveyRepository,
        mapStateRepository,
        settingsManager,
        permissionsManager,
        locationOfInterestRepository,
        networkManager,
      )
  }

  @Test
  fun `Initial state should have null bottomText and download button disabled`() =
    runWithTestDispatcher {
      assertNull(viewModel.uiState.value.bottomTextState)
      assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())
      assertEquals(
        OfflineAreaSelectorState.DownloadState.Idle,
        viewModel.uiState.value.downloadState,
      )
    }

  @Test
  fun `Should show download size correctly`() = runWithTestDispatcher {
    setupMocks(estimatedSizeOnDisk = Result.success(1024 * 1024 * 5))

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    assertEquals(
      OfflineAreaSelectorState.BottomTextState.AreaSize(5.0f.toMbString()),
      viewModel.uiState.value.bottomTextState,
    )
    assertEquals(true, viewModel.uiState.value.isDownloadButtonEnabled())
  }

  @Test
  fun `Should show download size for small areas correctly`() = runWithTestDispatcher {
    // Less than 1 MB → should display "<1"
    setupMocks(estimatedSizeOnDisk = Result.success(500_000))

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    assertEquals(
      OfflineAreaSelectorState.BottomTextState.AreaSize("<1"),
      viewModel.uiState.value.bottomTextState,
    )
    assertEquals(true, viewModel.uiState.value.isDownloadButtonEnabled())
  }

  @Test
  fun `Should show appropriate message when there is no imagery`() = runWithTestDispatcher {
    setupMocks(hasHiResImagery = Result.success(false))

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    assertEquals(
      OfflineAreaSelectorState.BottomTextState.NoImageryAvailable,
      viewModel.uiState.value.bottomTextState,
    )
    assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())
  }

  @Test
  fun `Should show appropriate message when there's a network error checking for high res imagery`() =
    runWithTestDispatcher {
      setupMocks(hasHiResImagery = Result.failure(SocketTimeoutException("timeout")))

      viewModel.onMapCameraMoved(CAMERA_POSITION)
      advanceUntilIdle()

      assertEquals(
        OfflineAreaSelectorState.BottomTextState.NetworkError,
        viewModel.uiState.value.bottomTextState,
      )
      assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())
    }

  @Test
  fun `Should show appropriate message when there's a network error checking for estimated imagery size`() =
    runWithTestDispatcher {
      setupMocks(estimatedSizeOnDisk = Result.failure(UnknownHostException("unknown")))

      viewModel.onMapCameraMoved(CAMERA_POSITION)
      advanceUntilIdle()

      assertEquals(
        OfflineAreaSelectorState.BottomTextState.NetworkError,
        viewModel.uiState.value.bottomTextState,
      )
      assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())
    }

  @Test
  fun `Should show area too large when zoom is too low`() = runWithTestDispatcher {
    setupMocks()
    val lowZoomPosition = CAMERA_POSITION.copy(zoomLevel = 5.0f)

    viewModel.onMapCameraMoved(lowZoomPosition)
    advanceUntilIdle()

    assertEquals(
      OfflineAreaSelectorState.BottomTextState.AreaTooLarge,
      viewModel.uiState.value.bottomTextState,
    )
    assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())
  }

  @Test
  fun `Should show area too large when estimated size exceeds max`() = runWithTestDispatcher {
    val largeSizeBytes = 1024 * 1024 * 51
    setupMocks(estimatedSizeOnDisk = Result.success(largeSizeBytes))

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    assertEquals(
      OfflineAreaSelectorState.BottomTextState.AreaTooLarge,
      viewModel.uiState.value.bottomTextState,
    )
    assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())
  }

  @Test
  fun `Should reset state on map drag`() = runWithTestDispatcher {
    setupMocks()
    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isDownloadButtonEnabled())

    viewModel.onMapDragged()

    assertNull(viewModel.uiState.value.bottomTextState)
    assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())
  }

  @Test
  fun `Should show download button enabled only for AreaSize state`() = runWithTestDispatcher {
    // AreaTooLarge
    setupMocks(estimatedSizeOnDisk = Result.success(1024 * 1024 * 51))
    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())

    // NoImageryAvailable
    setupMocks(hasHiResImagery = Result.success(false))
    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())

    // NetworkError
    setupMocks(hasHiResImagery = Result.failure(SocketTimeoutException("timeout")))
    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isDownloadButtonEnabled())

    // AreaSize (valid downloadable area)
    setupMocks(estimatedSizeOnDisk = Result.success(1024 * 1024 * 5))
    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isDownloadButtonEnabled())
  }

  @Test
  fun `onDownloadClick when network unavailable should emit NetworkUnavailable event`() =
    runWithTestDispatcher {
      setupMocks(isNetworkConnected = false)

      viewModel.uiEvent.test {
        viewModel.onDownloadClick()
        assertEquals(OfflineAreaSelectorEvent.NetworkUnavailable, awaitItem())
      }
    }

  @Test
  fun `onDownloadClick should emit DownloadError on download failure and return to home screen`() =
    runWithTestDispatcher {
      @Suppress("TooGenericExceptionThrown")
      val errorFlow = flow<Pair<Int, Int>> { throw RuntimeException("download failed") }
      setupMocks(downloadProgressFlow = errorFlow)

      viewModel.onMapCameraMoved(CAMERA_POSITION)
      advanceUntilIdle()

      viewModel.uiEvent.test {
        viewModel.onDownloadClick()
        advanceUntilIdle()

        assertEquals(OfflineAreaSelectorEvent.DownloadError, awaitItem())
        assertEquals(OfflineAreaSelectorEvent.NavigateOfflineAreaBackToHomeScreen, awaitItem())
      }
    }

  @Test
  fun `onDownloadClick should start download and update progress`() = runWithTestDispatcher {
    val progressFlow = MutableSharedFlow<Pair<Int, Int>>()
    setupMocks(downloadProgressFlow = progressFlow)

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    viewModel.onDownloadClick()
    advanceUntilIdle()

    progressFlow.emit(Pair(50, 100))
    advanceUntilIdle()

    val state = viewModel.uiState.value.downloadState
    assert(state is OfflineAreaSelectorState.DownloadState.InProgress)
    assertEquals(0.5f, (state as OfflineAreaSelectorState.DownloadState.InProgress).progress)
  }

  @Test
  fun `stopDownloading should cancel job and reset download state`() = runWithTestDispatcher {
    val progressFlow = MutableSharedFlow<Pair<Int, Int>>()
    setupMocks(downloadProgressFlow = progressFlow)

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    viewModel.onDownloadClick()
    advanceUntilIdle()

    progressFlow.emit(Pair(50, 100))
    advanceUntilIdle()

    viewModel.stopDownloading()

    assertNull(viewModel.downloadJob)
    assertEquals(OfflineAreaSelectorState.DownloadState.Idle, viewModel.uiState.value.downloadState)
  }

  @Test
  fun `onDownloadClick should navigate home after successful download`() = runWithTestDispatcher {
    val progressFlow = flow {
      emit(50 to 100)
      emit(100 to 100)
    }
    setupMocks(downloadProgressFlow = progressFlow)

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    viewModel.uiEvent.test {
      viewModel.onDownloadClick()
      advanceUntilIdle()

      assertEquals(OfflineAreaSelectorEvent.NavigateOfflineAreaBackToHomeScreen, awaitItem())
    }
  }

  @Test
  fun `onCancelClick should emit NavigateUp event`() = runWithTestDispatcher {
    viewModel.uiEvent.test {
      viewModel.onCancelClick()
      assertEquals(OfflineAreaSelectorEvent.NavigateUp, awaitItem())
    }
  }

  private suspend fun setupMocks(
    hasHiResImagery: Result<Boolean> = Result.success(true),
    estimatedSizeOnDisk: Result<Int> = Result.success(1024 * 1024 * 5),
    isNetworkConnected: Boolean = true,
    downloadProgressFlow: Flow<Pair<Int, Int>> = MutableSharedFlow(),
  ) {
    whenever(offlineAreaRepository.hasHiResImagery(any())).thenReturn(hasHiResImagery)
    whenever(offlineAreaRepository.estimateSizeOnDisk(any())).thenReturn(estimatedSizeOnDisk)
    whenever(networkManager.isNetworkConnected()).thenReturn(isNetworkConnected)
    whenever(offlineAreaRepository.downloadTiles(any())).thenReturn(downloadProgressFlow)
  }

  private companion object {
    val CAMERA_POSITION =
      CameraPosition(
        Coordinates(0.5, 0.5),
        10.0f,
        Bounds(Coordinates(0.0, 0.0), Coordinates(1.0, 1.0)),
      )
  }
}
