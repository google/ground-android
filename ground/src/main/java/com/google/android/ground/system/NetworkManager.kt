/*
 * Copyright 2019 Google LLC
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
import android.net.ConnectivityManager
import androidx.annotation.RequiresPermission
import java.net.ConnectException

/** Abstracts access to network state. */
object NetworkManager {

  /** Returns true iff the device has internet connectivity, false otherwise. */
  @RequiresPermission("android.permission.ACCESS_NETWORK_STATE")
  private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = cm.activeNetworkInfo
    return networkInfo?.isConnected ?: false
  }

  /** Throws an error if network isn't available. */
  fun requireActiveNetwork(context: Context) {
    if (!isNetworkAvailable(context)) {
      throw ConnectException()
    }
  }
}
