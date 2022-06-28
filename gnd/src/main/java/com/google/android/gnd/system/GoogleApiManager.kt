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
package com.google.android.gnd.system

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gnd.rx.RxCompletable
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import javax.inject.Inject

private val INSTALL_API_REQUEST_CODE = GoogleApiAvailability::class.java.hashCode() and 0xffff

@ActivityScoped
class GoogleApiManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val googleApiAvailability: GoogleApiAvailability,
    private val activityStreams: ActivityStreams
) {

    /**
     * Returns a stream that either completes immediately if Google Play Services are already
     * installed, otherwise shows install dialog. Terminates with error if install not possible or
     * cancelled.
     */
    fun installGooglePlayServices(): Completable =
        requestInstallOrComplete().ambWith(getNextInstallApiResult())

    private fun requestInstallOrComplete(): Completable =
        Completable.create { emitter: CompletableEmitter ->
            val status = googleApiAvailability.isGooglePlayServicesAvailable(context)
            if (status == ConnectionResult.SUCCESS) {
                emitter.onComplete()
            } else if (googleApiAvailability.isUserResolvableError(status)) {
                activityStreams.withActivity {
                    googleApiAvailability.showErrorDialogFragment(
                        it,
                        status,
                        INSTALL_API_REQUEST_CODE
                    ) { emitter.onError(Exception()) } // TODO: Throw appropriate Exception.
                }
            } else {
                emitter.onError(Exception()) // TODO: Throw appropriate Exception.
            }
        }

    private fun getNextInstallApiResult(): Completable =
        activityStreams
            .getNextActivityResult(INSTALL_API_REQUEST_CODE)
            .flatMapCompletable {
                RxCompletable.completeOrError(
                    { it.isOk() },
                    Exception::class.java // TODO: Throw appropriate Exception.
                )
            }
}
