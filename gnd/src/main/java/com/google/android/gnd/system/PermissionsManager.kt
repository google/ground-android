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

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gnd.rx.RxCompletable.completeIf
import com.google.android.gnd.rx.RxCompletable.completeOrError
import com.google.android.gnd.rx.annotations.Cold
import com.google.android.gnd.system.PermissionsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Provides access to obtain and check the app's permissions.  */
@Singleton
class PermissionsManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val activityStreams: ActivityStreams
) {
    /**
     * Asynchronously requests the app be granted the specified permission. If the permission has
     * already been granted, completes immediately, otherwise completes once the next permissions
     * result is received.
     */
    fun obtainPermission(permission: String): @Cold Completable =
        completeIf { requestPermission(permission) }.ambWith(getPermissionsResult(permission))

    /**
     * Sends the system request that the app be granted the specified permission. Returns `true`
     * if the permission was already granted.
     */
    private fun requestPermission(permission: String): Boolean =
        if (isGranted(permission)) {
            Timber.d("%s already granted", permission)
            true
        } else {
            Timber.d("Requesting %s", permission)
            activityStreams.withActivity { activity: Activity ->
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(permission),
                    PERMISSIONS_REQUEST_CODE
                )
            }
            false
        }

    /** Returns `true` iff the app has been granted the specified permission.  */
    private fun isGranted(permission: String): Boolean =
        checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Returns a [Completable] that completes once the specified permission is granted or
     * terminates with error [PermissionDeniedException] if the requested permission was denied.
     */
    private fun getPermissionsResult(permission: String): @Cold Completable =
        activityStreams
            .getNextRequestPermissionsResult(PERMISSIONS_REQUEST_CODE)
            .flatMapCompletable { r: RequestPermissionsResult ->
                completeOrError({ r.isGranted(permission) }, PermissionDeniedException::class.java)
            }

    companion object {
        @JvmField
        val PERMISSIONS_REQUEST_CODE = PermissionsManager::class.java.hashCode() and 0xffff
    }
}

/**
 * Indicates a request to grant the app permissions was denied. More specifically, it indicates
 * the requested permission was not in the list of granted permissions in the system's response.
 */
class PermissionDeniedException : Exception()
