/*
 * Copyright 2018 Google LLC
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

import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

val LOCATION_SETTINGS_REQUEST_CODE = SettingsManager::class.java.hashCode() and 0xffff

/**
 * Manages enabling of settings and related flows to/from the Activity.
 *
 * Note: Currently only supports location settings, but could be expanded to support other settings
 * types in the future.
 */
@Singleton
class SettingsManager
@Inject
constructor(
  private val activityStreams: ActivityStreams,
  private val settingsClient: SettingsClient,
) {

  /**
   * Try to enable location settings. If location settings are already enabled, this will complete
   * immediately on subscribe.
   */
  suspend fun enableLocationSettings(locationRequest: LocationRequest) {
    Timber.d("Checking location settings")
    val settingsRequest =
      LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
    try {
      settingsClient.checkLocationSettings(settingsRequest)
    } catch (e: Throwable) {
      onCheckSettingsFailure(e)
    }
  }

  private suspend fun onCheckSettingsFailure(throwable: Throwable) =
    if (throwable is ResolvableApiException) {
      val requestCode = LOCATION_SETTINGS_REQUEST_CODE
      startResolution(requestCode, throwable)
      getNextResult(requestCode)
    } else {
      throw throwable
    }

  private fun startResolution(requestCode: Int, resolvableException: ResolvableApiException) {
    Timber.d("Prompting user to enable settings")
    activityStreams.withActivity { resolvableException.startResolutionForResult(it, requestCode) }
  }

  private suspend fun getNextResult(requestCode: Int) {
    val result = activityStreams.getNextActivityResult(requestCode)
    if (!result.isOk()) {
      throw SettingsChangeRequestCanceled()
    }
  }
}

class SettingsChangeRequestCanceled : Exception()
