/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.system

import android.location.Location
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocationManagerTest : BaseHiltTest() {

  private val lastLocation = Location("test_provider")

  @BindValue @Mock lateinit var locationClient: FusedLocationProviderClient
  @Inject lateinit var locationManager: LocationManager

  @Test
  fun `Request location updates when last location available`() = runWithTestDispatcher {
    whenever(locationClient.getLastLocation()).thenReturn(lastLocation)

    locationManager.requestLocationUpdates()

    verify(locationClient, times(1)).getLastLocation()
    verify(locationClient, times(1)).requestLocationUpdates(any(), any())
    locationManager.locationUpdates.test {
      assertThat(expectMostRecentItem()).isEqualTo(lastLocation)
    }
  }

  @Test
  fun `Request location updates when last location missing`() = runWithTestDispatcher {
    whenever(locationClient.getLastLocation()).thenReturn(null)

    locationManager.requestLocationUpdates()

    verify(locationClient, times(1)).getLastLocation()
    verify(locationClient, times(1)).requestLocationUpdates(any(), any())
    locationManager.locationUpdates.test { expectNoEvents() }
  }

  @Test
  fun `Disable location updates`() = runWithTestDispatcher {
    locationManager.disableLocationUpdates()

    verify(locationClient, times(1)).removeLocationUpdates(any())
  }
}
