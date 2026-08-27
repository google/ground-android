/*
 * Copyright 2026 Google LLC
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

package org.groundplatform.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.groundplatform.domain.system.NetworkManagerInterface
import org.groundplatform.domain.system.NetworkStatus

/** In-memory implementation of [NetworkManagerInterface] for unit testing. */
class FakeNetworkManager(initialStatus: NetworkStatus = NetworkStatus.AVAILABLE) :
  NetworkManagerInterface {
  val networkStatusStateFlow = MutableStateFlow(initialStatus)
  override val networkStatusFlow: Flow<NetworkStatus> = networkStatusStateFlow

  override fun isNetworkConnected(): Boolean =
    networkStatusStateFlow.value == NetworkStatus.AVAILABLE

  fun setNetworkStatus(status: NetworkStatus) {
    networkStatusStateFlow.value = status
  }
}
