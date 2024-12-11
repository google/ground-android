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
package com.google.android.ground.system

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val INSTALL_API_REQUEST_CODE = GoogleApiAvailability::class.java.hashCode() and 0xffff

@Singleton
class GoogleApiManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val googleApiAvailability: GoogleApiAvailability,
  private val activityStreams: ActivityStreams,
) {

  /**
   * Displays a dialog to install Google Play Services, if missing. Throws an error if install not
   * possible or cancelled.
   */
  suspend fun installGooglePlayServices() {
    val status = googleApiAvailability.isGooglePlayServicesAvailable(context)
    if (status != ConnectionResult.SUCCESS) {
      if (!resolveError(status, INSTALL_API_REQUEST_CODE))
        throw GooglePlayServicesNotAvailableException(status)
    }
  }

  /**
   * Attempts to resolve the error indicated by the given `status` code, using the provided
   * `requestCode` to uniquely identify Activity callbacks.
   */
  private suspend fun resolveError(status: Int, requestCode: Int): Boolean {
    if (!googleApiAvailability.isUserResolvableError(status)) return false

    activityStreams.withActivity { activity ->
      googleApiAvailability.showErrorDialogFragment(activity, status, requestCode)
    }
    // `isOk()` returns `false` if install failed or was cancelled.
    return activityStreams.getNextActivityResult(requestCode).isOk()
  }
}
