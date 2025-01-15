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

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Provides access to obtain and check the app's permissions. */
@Singleton
class PermissionsManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val activityStreams: ActivityStreams,
) {
  /**
   * Asynchronously requests the app be granted the specified permission. If the permission has
   * already been granted, completes immediately, otherwise completes once the next permissions
   * result is received.
   */
  suspend fun obtainPermission(permission: String) {
    if (!requestPermission(permission)) {
      getPermissionsResult(permission)
    }
  }

  /**
   * Sends the system request that the app be granted the specified permission. Returns `true` if
   * the permission was already granted.
   */
  private fun requestPermission(permission: String): Boolean =
    if (isGranted(permission)) {
      Timber.d("%s already granted", permission)
      true
    } else {
      Timber.d("Requesting %s", permission)
      activityStreams.withActivity { activity: Activity ->
        ActivityCompat.requestPermissions(activity, arrayOf(permission), PERMISSIONS_REQUEST_CODE)
      }
      false
    }

  /** Returns `true` if the app has been granted the specified permission. */
  fun isGranted(permission: String): Boolean =
    checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

  /** Throws an error [PermissionDeniedException] if the request permission was denied. */
  private suspend fun getPermissionsResult(permission: String) {
    val result = activityStreams.getNextRequestPermissionsResult(PERMISSIONS_REQUEST_CODE)
    if (!result.isGranted(permission)) {
      throw PermissionDeniedException("Permission denied: $permission")
    }
  }

  companion object {
    @JvmField val PERMISSIONS_REQUEST_CODE = PermissionsManager::class.java.hashCode() and 0xffff
  }
}

/**
 * Indicates a request to grant the app permissions was denied. More specifically, it indicates the
 * requested permission was not in the list of granted permissions in the system's response.
 */
class PermissionDeniedException(message: String) : Exception(message)
