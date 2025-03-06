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

import android.app.Activity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.BaseHiltTest
import org.junit.Assert.assertThrows
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
class SettingsManagerTest : BaseHiltTest() {

  private val testLocationRequest = LocationRequest()

  @BindValue @Mock lateinit var activityStreamsMock: ActivityStreams
  @BindValue @Mock lateinit var settingsClientMock: SettingsClient

  @Inject lateinit var settingsManager: SettingsManager

  @Test
  fun `enableLocationSettings() checks system settings`() = runWithTestDispatcher {
    settingsManager.enableLocationSettings(testLocationRequest)

    verify(settingsClientMock, times(1)).checkLocationSettings(any())
    verify(activityStreamsMock, times(0)).withActivity(any())
    verify(activityStreamsMock, times(0)).getNextActivityResult(any())
  }

  @Test
  fun `enableLocationSettings() attempts to resolve error if resolvable`() = runWithTestDispatcher {
    whenever(activityStreamsMock.getNextActivityResult(LOCATION_SETTINGS_REQUEST_CODE))
      .thenReturn(ActivityResult(LOCATION_SETTINGS_REQUEST_CODE, Activity.RESULT_OK, null))

    whenever(settingsClientMock.checkLocationSettings(any())).thenAnswer {
      throw ResolvableApiException(Status.RESULT_INTERNAL_ERROR)
    }

    settingsManager.enableLocationSettings(testLocationRequest)

    verify(activityStreamsMock, times(1)).withActivity(any())
    verify(activityStreamsMock, times(1)).getNextActivityResult(any())
  }

  @Test
  fun `enableLocationSettings() throws error if non-resolvable`() = runWithTestDispatcher {
    whenever(settingsClientMock.checkLocationSettings(any()))
      .thenThrow(IllegalStateException::class.java)

    assertThrows(IllegalStateException::class.java) {
      runBlocking { settingsManager.enableLocationSettings(testLocationRequest) }
    }

    verify(activityStreamsMock, times(0)).withActivity(any())
    verify(activityStreamsMock, times(0)).getNextActivityResult(any())
  }
}
