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
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE
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
  fun `Should display the correct location icon and hide the recenter button when the location is locked`() =
    runTest {
      setupMocks(isLocationLocked = true)

      val iconType = viewModel.locationLockIconType.first()
      val showRecenter = viewModel.shouldShowRecenterButton.first()
      assert(iconType is MapFloatingActionButtonType.LocationLocked)
      assertEquals(false, showRecenter)
    }

  @Test
  fun `Should display the correct location icon and show the recenter button when the location is not locked`() =
    runTest {
      setupMocks(isLocationLocked = false)

      val iconType = viewModel.locationLockIconType.first()
      val showRecenter = viewModel.shouldShowRecenterButton.first()
      assert(iconType is MapFloatingActionButtonType.LocationNotLocked)
      assertEquals(true, showRecenter)
    }

  @Test
  fun `Should display the correct icon and hide the recenter button if location permissions were not granted`() =
    runTest {
      setupMocks(isLocationLocked = false, hasLocationPermissions = false)

      val iconType = viewModel.locationLockIconType.first()
      val showRecenter = viewModel.shouldShowRecenterButton.first()
      assert(iconType is MapFloatingActionButtonType.LocationNotLocked)
      assertEquals(false, showRecenter)
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
  fun `Should enable location lock correctly and receive location updates`() = runTest {
    setupMocks()

    viewModel.enableLocationLockAndGetUpdates()

    assertEquals(true, viewModel.locationLock.value.getOrNull())
    verify(permissionsManager).obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    verify(settingsManager).enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST)
    verify(locationManager).requestLocationUpdates()
  }

  @Test
  fun `Should fallback and receive location updates when location settings fails with SETTINGS_CHANGE_UNAVAILABLE`() =
    runTest {
      val apiException = ApiException(Status(SETTINGS_CHANGE_UNAVAILABLE))
      setupMocks(
        enableLocationSettingsException = apiException,
        hasLocationPermissions = true,
        isLocationLocked = false,
      )

      viewModel.enableLocationLockAndGetUpdates()

      assertEquals(true, viewModel.locationLock.value.getOrNull())
      verify(permissionsManager).obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION)
      verify(locationManager).requestLocationUpdates()
    }

  @Test
  fun `Should not enable location lock and disable location updates if location settings fails with other exception`() =
    runTest {
      val apiException = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
      setupMocks(
        enableLocationSettingsException = apiException,
        hasLocationPermissions = true,
        isLocationLocked = false,
      )

      viewModel.enableLocationLockAndGetUpdates()

      assertEquals(Result.failure<Boolean>(apiException), viewModel.locationLock.value)
      verify(permissionsManager).obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION)
      verify(locationManager).disableLocationUpdates()
    }

  @Test
  fun `Should disable location lock on map drag and stop receiving location updates`() = runTest {
    setupMocks()

    viewModel.onLocationLockClick()
    viewModel.onMapDragged()

    assertEquals(false, viewModel.locationLock.value.getOrNull())
    verify(locationManager).disableLocationUpdates()
  }

  private fun setupMocks(
    isLocationLocked: Boolean = false,
    hasLocationPermissions: Boolean = true,
    enableLocationSettingsException: ApiException? = null,
  ) = runBlocking {
    whenever(mapStateRepository.isLocationLockEnabled).thenReturn(isLocationLocked)

    enableLocationSettingsException?.let {
      whenever(settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST)).thenAnswer {
        throw enableLocationSettingsException
      }
    }
      ?: run {
        whenever(settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST))
          .thenReturn(Unit)
      }

    whenever(permissionsManager.obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION))
      .thenReturn(Unit)
    whenever(permissionsManager.isGranted(Manifest.permission.ACCESS_FINE_LOCATION))
      .thenReturn(hasLocationPermissions)

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
