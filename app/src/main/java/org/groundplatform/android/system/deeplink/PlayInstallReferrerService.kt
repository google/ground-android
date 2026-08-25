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
package org.groundplatform.android.system.deeplink

import android.content.Context
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.groundplatform.android.data.local.LocalValueStore
import timber.log.Timber

@Singleton
class PlayInstallReferrerService
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val localValueStore: LocalValueStore,
) {
  suspend fun getDeferredSurveyId(): String? {
    if (localValueStore.isDeferredDeeplinkConsumed) return null
    val result = withTimeoutOrNull(QUERY_TIMEOUT_MILLIS.milliseconds) { queryInstallReferrer() }
    if (result == null) {
      Timber.w("Timed out reading install referrer")
    }
    return when (result) {
      is InstallReferrerResult.Success -> {
        localValueStore.isDeferredDeeplinkConsumed = true
        parseSurveyId(result.referrer)
      }
      // Play can't serve install referrers on this device, so retrying will never help
      InstallReferrerResult.Unsupported -> {
        localValueStore.isDeferredDeeplinkConsumed = true
        null
      }
      // Transient failure. Leave the referrer eligible so the next launch can retry it, since it
      // stays retrievable from Play for a while after install.
      InstallReferrerResult.Unavailable,
      null -> {
        null
      }
    }
  }

  private suspend fun queryInstallReferrer(): InstallReferrerResult {
    val client = InstallReferrerClient.newBuilder(context).build()
    val result = CompletableDeferred<InstallReferrerResult>()
    return try {
      val listener =
        object : InstallReferrerStateListener {
          override fun onInstallReferrerSetupFinished(responseCode: Int) {
            result.complete(readReferrer(client, responseCode))
          }

          override fun onInstallReferrerServiceDisconnected() {
            result.complete(InstallReferrerResult.Unavailable)
          }
        }
      runCatching { client.startConnection(listener) }
        .onFailure { throwable ->
          Timber.e(throwable, "Failed to start install referrer connection")
          result.complete(InstallReferrerResult.Unavailable)
        }
      result.await()
    } finally {
      finish(client)
    }
  }

  private fun readReferrer(
    client: InstallReferrerClient,
    responseCode: Int,
  ): InstallReferrerResult {
    if (responseCode != InstallReferrerClient.InstallReferrerResponse.OK) {
      Timber.w("Install referrer setup failed with response code: $responseCode")
      return if (
        responseCode == InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED
      ) {
        InstallReferrerResult.Unsupported
      } else {
        InstallReferrerResult.Unavailable
      }
    }
    return runCatching { InstallReferrerResult.Success(client.installReferrer.installReferrer) }
      .getOrElse {
        Timber.e(it, "Failed to read install referrer")
        InstallReferrerResult.Unavailable
      }
  }

  internal fun parseSurveyId(referrer: String): String? =
    referrer.split('&').firstNotNullOfOrNull { param ->
      if (param.substringBefore('=', "") == DEFERRED_SURVEY_ID_KEY) {
        Uri.decode(param.substringAfter('=')).takeIf { it.isNotBlank() }
      } else {
        null
      }
    }

  private fun finish(client: InstallReferrerClient) {
    runCatching { client.endConnection() }
      .onFailure { Timber.e(it, "Failed to close install referrer connection") }
  }

  private sealed interface InstallReferrerResult {
    data class Success(val referrer: String) : InstallReferrerResult

    data object Unavailable : InstallReferrerResult

    data object Unsupported : InstallReferrerResult
  }

  companion object {
    private const val DEFERRED_SURVEY_ID_KEY = "survey_id"
    private const val QUERY_TIMEOUT_MILLIS = 3000L
  }
}
