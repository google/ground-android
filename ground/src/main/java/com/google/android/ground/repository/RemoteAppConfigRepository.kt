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

import com.google.android.ground.model.RemoteAppConfig
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.remote.RemoteDataStore
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val LOAD_REMOTE_APP_CONFIG_TIMEOUT_MILLIS: Long = 30 * 1000

@Singleton
class RemoteAppConfigRepository
@Inject
constructor(
  private val remoteDataStore: RemoteDataStore,
  private val localValueStore: LocalValueStore
) {

  // TODO: Move into appropriate repo.
  var isTermsOfServiceAccepted: Boolean by localValueStore::isTermsOfServiceAccepted

  /** Throws [kotlinx.coroutines.TimeoutCancellationException] if loading timed out. */
  suspend fun getRemoteAppConfig(): RemoteAppConfig =
    withTimeout(LOAD_REMOTE_APP_CONFIG_TIMEOUT_MILLIS) {
      Timber.d("Loading remote app config...")
      remoteDataStore.loadRemoteAppConfig()
    }
}
