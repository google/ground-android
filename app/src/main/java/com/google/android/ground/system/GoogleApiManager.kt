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
import com.google.android.gms.common.ConnectionResult.SUCCESS
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.delay
import timber.log.Timber

private val INSTALL_API_REQUEST_CODE = GoogleApiAvailability::class.java.hashCode() and 0xffff
private const val PLAY_SERVICES_RETRY_DELAY_MS = 2500L

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
    val status = isGooglePlayServicesAvailable()
    if (status == SUCCESS) return
    if (googleApiAvailability.isUserResolvableError(status)) {
      showErrorDialog(status, INSTALL_API_REQUEST_CODE)
    } else {
      throw GooglePlayServicesNotAvailableException(status)
    }
    // onActivityResult() is sometimes called with a failure prematurely or not at all. Instead, we
    // poll for Play services.
    while (isGooglePlayServicesAvailable() != SUCCESS) {
      Timber.d("Waiting for Play services")
      delay(PLAY_SERVICES_RETRY_DELAY_MS)
    }
  }

  private fun isGooglePlayServicesAvailable(): Int =
    googleApiAvailability.isGooglePlayServicesAvailable(context)

  /**
   * Attempts to resolve the error indicated by the given `status` code, using the provided
   * `requestCode` to differentiate Activity callbacks from others. Suspends until the dialog is
   * dismissed.
   */
  private suspend fun showErrorDialog(status: Int, requestCode: Int) =
    suspendCoroutine { continuation ->
      activityStreams.withActivity { activity ->
        val dialog = googleApiAvailability.getErrorDialog(activity, status, requestCode)
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setOnDismissListener { continuation.resume(Unit) }
        dialog?.show()
      }
    }
}
