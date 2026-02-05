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
@file:OptIn(ExperimentalCoroutinesApi::class)

package org.groundplatform.android.ui.offlineareas.selector

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidTest
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.map.Bounds
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.util.toMbString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorViewModelTest : BaseHiltTest() {

  @Mock private lateinit var offlineAreaRepository: OfflineAreaRepository
  @Mock private lateinit var locationManager: LocationManager
  @Mock private lateinit var surveyRepository: SurveyRepository
  @Mock private lateinit var mapStateRepository: MapStateRepository
  @Mock private lateinit var settingsManager: SettingsManager
  @Mock private lateinit var permissionsManager: PermissionsManager
  @Mock private lateinit var locationOfInterestRepository: LocationOfInterestRepository
  @Mock private lateinit var networkManager: NetworkManager

  private lateinit var resources: Resources

  private lateinit var viewModel: OfflineAreaSelectorViewModel

  @Before
  override fun setUp() {
    super.setUp()
    val context = ApplicationProvider.getApplicationContext<Context>()
    resources = context.resources
    Dispatchers.setMain(testDispatcher)
    viewModel =
      OfflineAreaSelectorViewModel(
        offlineAreaRepository,
        testDispatcher,
        resources,
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
  fun `Should show download size correctly`() = runTest {
    setupMocks(estimatedSizeOnDisk = Result.success(1024 * 1024 * 5))

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    assertEquals(
      resources.getString(R.string.selected_offline_area_size, (5.0f).toMbString()),
      viewModel.bottomText.value,
    )
    assertEquals(true, viewModel.downloadButtonEnabled.value)
  }

  @Test
  fun `Should show appropriate message when there is no imagery`() = runTest {
    setupMocks(hasHiResImagery = Result.success(false))

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    assertEquals(
      resources.getString(R.string.no_imagery_available_for_area),
      viewModel.bottomText.value,
    )
    assertEquals(false, viewModel.downloadButtonEnabled.value)
  }

  @Test
  fun `Should show appropriate message when there's a network error checking for high res imagery`() =
    runTest {
      setupMocks(hasHiResImagery = Result.failure(SocketTimeoutException("timeout")))

      viewModel.onMapCameraMoved(CAMERA_POSITION)
      advanceUntilIdle()

      assertEquals(
        resources.getString(R.string.connect_to_download_message),
        viewModel.bottomText.value,
      )
      assertEquals(false, viewModel.downloadButtonEnabled.value)
    }

  @Test
  fun `Should show appropriate message when there's a network error checking for estimated imagery size`() =
    runTest {
      setupMocks(estimatedSizeOnDisk = Result.failure(UnknownHostException("unknown")))

      viewModel.onMapCameraMoved(CAMERA_POSITION)
      advanceUntilIdle()

      assertEquals(
        resources.getString(R.string.connect_to_download_message),
        viewModel.bottomText.value,
      )
      assertEquals(false, viewModel.downloadButtonEnabled.value)
    }

  private suspend fun setupMocks(
    hasHiResImagery: Result<Boolean> = Result.success(true),
    estimatedSizeOnDisk: Result<Int> = Result.success(1024 * 1024 * 5),
  ) {
    whenever(offlineAreaRepository.hasHiResImagery(any())).thenReturn(hasHiResImagery)
    whenever(offlineAreaRepository.estimateSizeOnDisk(any())).thenReturn(estimatedSizeOnDisk)
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
