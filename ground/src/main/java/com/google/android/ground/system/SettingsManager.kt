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

import android.content.IntentSender.SendIntentException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.ground.rx.RxCompletable.completeOrError
import com.google.android.ground.system.rx.RxSettingsClient
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

private val LOCATION_SETTINGS_REQUEST_CODE = SettingsManager::class.java.hashCode() and 0xffff

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
  private val settingsClient: RxSettingsClient
) {

  /**
   * Try to enable location settings. If location settings are already enabled, this will complete
   * immediately on subscribe.
   */
  fun enableLocationSettings(locationRequest: LocationRequest): Completable {
    Timber.d("Checking location settings")
    val settingsRequest =
      LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
    return settingsClient.checkLocationSettings(settingsRequest).toCompletable().onErrorResumeNext {
      onCheckSettingsFailure(it)
    }
  }

  private fun onCheckSettingsFailure(throwable: Throwable): Completable =
    if (throwable is ResolvableApiException) {
      val requestCode = LOCATION_SETTINGS_REQUEST_CODE
      startResolution(requestCode, throwable).andThen(getNextResult(requestCode))
    } else {
      Completable.error(throwable)
    }

  private fun startResolution(
    requestCode: Int,
    resolvableException: ResolvableApiException
  ): Completable =
    Completable.create { emitter: CompletableEmitter ->
      Timber.d("Prompting user to enable settings")
      activityStreams.withActivity {
        try {
          resolvableException.startResolutionForResult(it, requestCode)
          emitter.onComplete()
        } catch (e: SendIntentException) {
          emitter.onError(e)
        }
      }
    }

  private fun getNextResult(requestCode: Int): Completable =
    activityStreams
      .getNextActivityResult(requestCode)
      .flatMapCompletable {
        completeOrError({ it.isOk() }, SettingsChangeRequestCanceled::class.java)
      }
      .doOnComplete { Timber.d("Settings change request successful") }
      .doOnError { Timber.e(it, "Settings change request failed") }
}

class SettingsChangeRequestCanceled : Exception()
