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

package org.groundplatform.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withTimeout
import org.groundplatform.data.DataStoreException
import org.groundplatform.data.stores.LocalValueStore
import org.groundplatform.data.stores.RemoteDataStore
import org.groundplatform.domain.model.TermsOfService
import org.groundplatform.domain.repository.TermsOfServiceRepositoryInterface
import org.groundplatform.domain.system.NetworkManagerInterface

private const val LOAD_REMOTE_SURVEY_TERMS_OF_SERVICE_TIMEOUT_MILLIS: Long = 30 * 1000

class TermsOfServiceRepository(
  private val networkManager: NetworkManagerInterface,
  private val remoteDataStore: RemoteDataStore,
  private val localValueStore: LocalValueStore,
) : TermsOfServiceRepositoryInterface {

  override var isTermsOfServiceAccepted: Boolean by localValueStore::isTermsOfServiceAccepted

  /**
   * Fetches and returns [TermsOfService] from remote data store. Throws an error if the request
   * times out or network is unavailable.
   */
  override suspend fun getTermsOfService(): TermsOfService? {
    if (!networkManager.isNetworkConnected()) {
      throw DataStoreException("No network")
    }

    return withTimeout(LOAD_REMOTE_SURVEY_TERMS_OF_SERVICE_TIMEOUT_MILLIS) {
      Logger.d("Loading Terms of Service")
      remoteDataStore.loadTermsOfService()
    }
  }
}
