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
package org.groundplatform.android.ui.common

import android.Manifest
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.system.FINE_LOCATION_UPDATES_REQUEST
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class BaseMapViewModelTest : BaseHiltTest() {

  @Mock lateinit var locationManager: LocationManager
  @Mock lateinit var mapStateRepository: MapStateRepository
  @Mock lateinit var settingsManager: SettingsManager
  @Mock lateinit var offlineAreaRepository: OfflineAreaRepository
  @Mock lateinit var permissionsManager: PermissionsManager
  @Mock lateinit var surveyRepository: SurveyRepository
  @Mock lateinit var locationOfInterestRepository: LocationOfInterestRepository

  private lateinit var viewModel: BaseMapViewModel

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `Should display the correct locationLockIconType when the location is locked`() = runTest {
    setupMocks(true)

    val iconType = viewModel.locationLockIconType.first()
    assert(iconType is MapFloatingActionButtonType.LocationLocked)
  }

  @Test
  fun `Should display the correct locationLockIconType when the location is not locked`() =
    runTest {
      setupMocks(false)

      val iconType = viewModel.locationLockIconType.first()
      assert(iconType is MapFloatingActionButtonType.LocationNotLocked)
    }

  @Test
  fun `Should update map actions visibility when job selection modal visibility changes`() =
    runTest {
      setupMocks()

      viewModel.onJobSelectionModalVisibilityChanged(isShown = true)
      assertEquals(false, viewModel.shouldShowMapActions.first())

      viewModel.onJobSelectionModalVisibilityChanged(isShown = false)
      assertEquals(true, viewModel.shouldShowMapActions.first())
    }

  @Test
  fun `Should enable location lock correctly and receive location updates`() =
    runWithTestDispatcher {
      setupMocks()

      viewModel.enableLocationLockAndGetUpdates()

      assertEquals(true, viewModel.locationLock.value.getOrNull())
      verify(locationManager).requestLocationUpdates()
    }

  @Test
  fun `Should disable location lock on map drag and stop receiving location updates`() =
    runWithTestDispatcher {
      setupMocks()

      viewModel.onLocationLockClick()
      viewModel.onMapDragged()

      assertEquals(false, viewModel.locationLock.value.getOrNull())
      verify(locationManager).disableLocationUpdates()
    }

  private fun setupMocks(isLocationLocked: Boolean = false) = runBlocking {
    whenever(mapStateRepository.isLocationLockEnabled).thenReturn(isLocationLocked)

    whenever(settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST)).thenReturn(Unit)
    whenever(permissionsManager.obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION))
      .thenReturn(Unit)

    viewModel =
      BaseMapViewModel(
        locationManager,
        mapStateRepository,
        settingsManager,
        offlineAreaRepository,
        permissionsManager,
        surveyRepository,
        locationOfInterestRepository,
      )
  }
}
