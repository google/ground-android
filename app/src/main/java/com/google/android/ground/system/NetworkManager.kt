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
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

enum class NetworkStatus {
  AVAILABLE,
  UNAVAILABLE,
}

/** Abstracts access to network state. */
@Singleton
class NetworkManager @Inject constructor(@ApplicationContext private val context: Context) {
  val networkStatusFlow = initNetworkStatusFlow()

  private fun initNetworkStatusFlow(): Flow<NetworkStatus> {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    return callbackFlow {
      val callback =
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            trySend(NetworkStatus.AVAILABLE)
          }

          override fun onLost(network: Network) {
            trySend(NetworkStatus.UNAVAILABLE)
          }
        }

      val request = NetworkRequest.Builder().addCapability(NET_CAPABILITY_INTERNET).build()
      // Emit initial state.
      trySend(if (isNetworkConnected()) NetworkStatus.AVAILABLE else NetworkStatus.UNAVAILABLE)
      connectivityManager.registerNetworkCallback(request, callback)

      awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
  }

  /** Returns true if the device has internet connectivity, false otherwise. */
  @RequiresPermission("android.permission.ACCESS_NETWORK_STATE")
  fun isNetworkConnected(): Boolean {
    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

    val isConnected = networkCapabilities?.hasCapability(NET_CAPABILITY_INTERNET) ?: false
    Timber.d("Network connected: $isConnected")
    return isConnected
  }
}
