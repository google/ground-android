/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.map

import android.location.Location
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.repository.MapsRepository
import com.google.android.ground.system.LocationManager
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocationControllerTest : BaseHiltTest() {

  @Inject lateinit var mapsRepository: MapsRepository

  @Mock lateinit var locationManager: LocationManager

  private lateinit var locationController: LocationController

  @Before
  override fun setUp() {
    super.setUp()
    `when`(locationManager.getLocationUpdates()).thenReturn(Flowable.just(TEST_LOCATION))
    `when`(locationManager.enableLocationUpdates()).thenReturn(Single.just(RESULT_ENABLE_LOCK))
    `when`(locationManager.disableLocationUpdates()).thenReturn(Single.just(RESULT_DISABLE_LOCK))

    locationController = LocationController(locationManager, mapsRepository)
  }

  @Test
  fun testLocationUpdates_defaultLocationState() {
    locationController.getLocationLockUpdates().test().assertValue(RESULT_DISABLE_LOCK)
    locationController.getLocationUpdates().test().assertNoValues()
  }

  @Test
  fun testLocationUpdates_whenLastLocationStateIsLocked() {
    mapsRepository.isLocationLocked = true

    locationController.getLocationLockUpdates().test().assertValue(RESULT_ENABLE_LOCK)
    locationController.getLocationUpdates().test().assertValue(TEST_LOCATION)
  }

  @Test
  fun testLocationUpdates_whenLockRequest() {
    locationController.lock()

    locationController.getLocationLockUpdates().test().assertValues(RESULT_ENABLE_LOCK)
    locationController.getLocationUpdates().test().assertValue(TEST_LOCATION)
  }

  @Test
  fun testLocationUpdates_whenUnlockRequest() {
    locationController.unlock()

    locationController.getLocationLockUpdates().test().assertValues(RESULT_DISABLE_LOCK)
    locationController.getLocationUpdates().test().assertNoValues()
  }

  companion object {
    private val TEST_LOCATION = Location("test provider")
    private val RESULT_ENABLE_LOCK = Result.success(true)
    private val RESULT_DISABLE_LOCK = Result.success(false)
  }
}
