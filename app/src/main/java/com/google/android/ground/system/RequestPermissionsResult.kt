/*
 * Copyright 2021 Google LLC
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

import android.content.pm.PackageManager

/** Represents the arguments of an [Activity.onActivityResult] event. */
class RequestPermissionsResult
internal constructor(val requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
  private val permissionGrantResults: Map<String, Int> =
    IntRange(0, permissions.size - 1).associate { Pair(permissions[it], grantResults[it]) }

  /**
   * Returns true iff the event indicated the specified permission is granted to the application.
   */
  fun isGranted(permission: String): Boolean =
    permissionGrantResults[permission] == PackageManager.PERMISSION_GRANTED
}
