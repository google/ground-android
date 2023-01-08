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
import com.google.android.ground.system.LocationManager
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Flowable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocationControllerTest : BaseHiltTest() {
  @Mock lateinit var locationManager: LocationManager

  private lateinit var locationController: LocationController

  @Before
  override fun setUp() {
    super.setUp()
    locationController = LocationController(locationManager)
  }

  @Test
  fun testGetLocationLockUpdates_default() {
    locationController.getLocationLockUpdates().test().assertNoValues()
  }

  @Test
  fun testGetLocationLockUpdates_whenLockRequest() {
    setupEnableLocationUpdates()

    val locationLockUpdatesSubscriber = locationController.getLocationLockUpdates().test()
    locationController.lock()

    locationLockUpdatesSubscriber.assertValue { it.getOrNull() == true }
  }

  @Test
  fun testGetLocationLockUpdates_whenUnlockRequest() {
    setupDisableLocationUpdates()

    val locationLockUpdatesSubscriber = locationController.getLocationLockUpdates().test()
    locationController.unlock()

    locationLockUpdatesSubscriber.assertValue { it.getOrNull() == false }
  }

  @Test
  fun testGetLocationUpdates_whenLocked() {
    setupTestLocation()
    setupEnableLocationUpdates()

    val locationUpdatesSubscriber = locationController.getLocationUpdates().test()
    locationController.lock()

    locationUpdatesSubscriber.assertValue { it == TEST_LOCATION }
  }

  @Test
  fun testGetLocationUpdates_whenUnlocked() {
    setupTestLocation()
    setupDisableLocationUpdates()

    val locationUpdatesSubscriber = locationController.getLocationUpdates().test()
    locationController.unlock()

    locationUpdatesSubscriber.assertNoValues()
  }

  private fun setupTestLocation() {
    Mockito.`when`(locationManager.getLocationUpdates()).thenReturn(Flowable.just(TEST_LOCATION))
  }

  private fun setupEnableLocationUpdates() {
    Mockito.`when`(locationManager.enableLocationUpdates())
      .thenReturn(Single.just(Result.success(true)))
  }

  private fun setupDisableLocationUpdates() {
    Mockito.`when`(locationManager.disableLocationUpdates())
      .thenReturn(Single.just(Result.success(false)))
  }

  companion object {
    private val TEST_LOCATION = Location("test provider")
  }
}
