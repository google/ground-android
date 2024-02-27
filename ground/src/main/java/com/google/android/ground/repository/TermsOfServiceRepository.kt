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

package com.google.android.ground.repository

import com.google.android.ground.model.TermsOfService
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.remote.RemoteDataStore
import com.google.android.ground.system.NetworkManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val LOAD_REMOTE_SURVEY_TERMS_OF_SERVICE_TIMEOUT_MILLIS: Long = 30 * 1000

@Singleton
class TermsOfServiceRepository
@Inject
constructor(
  private val networkManager: NetworkManager,
  private val remoteDataStore: RemoteDataStore,
  private val localValueStore: LocalValueStore
) {

  var isTermsOfServiceAccepted: Boolean by localValueStore::isTermsOfServiceAccepted

  /**
   * @return [TermsOfService] from remote data store. Otherwise null if the request times out or
   *   network is unavailable.
   */
  suspend fun getTermsOfService(): TermsOfService? {
    // TODO(#1691): Maybe parse the exception and display to the user.
    if (!networkManager.isNetworkConnected()) {
      return null
    }

    return withTimeoutOrNull(LOAD_REMOTE_SURVEY_TERMS_OF_SERVICE_TIMEOUT_MILLIS) {
      Timber.d("Loading Terms of Service")
      remoteDataStore.loadTermsOfService()
    }
  }
}
