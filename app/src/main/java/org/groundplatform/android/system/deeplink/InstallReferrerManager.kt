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
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.groundplatform.android.data.local.LocalValueStore
import timber.log.Timber

@Singleton
class InstallReferrerManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val localValueStore: LocalValueStore,
) {
  suspend fun getDeferredSurveyId(): String? {
    if (localValueStore.isDeferredDeeplinkConsumed) return null
    return when (val result = queryInstallReferrer()) {
      is InstallReferrerResult.Success -> {
        if (!result.referrer.isEmpty()) {
          localValueStore.isDeferredDeeplinkConsumed = true
          parseSurveyId(Uri.decode(result.referrer))
        } else null
      }
      InstallReferrerResult.Unavailable -> null
    }
  }

  private suspend fun queryInstallReferrer(): InstallReferrerResult = suspendCancellableCoroutine {
    val client = InstallReferrerClient.newBuilder(context).build()
    val listener =
      object : InstallReferrerStateListener {
        override fun onInstallReferrerSetupFinished(responseCode: Int) {
          val result = readReferrer(client, responseCode)
          client.endConnection()
          it.resume(result)
        }

        override fun onInstallReferrerServiceDisconnected() {
          it.resume(InstallReferrerResult.Unavailable)
        }
      }
    runCatching { client.startConnection(listener) }
      .onFailure { throwable ->
        Timber.e(throwable, "Failed to start install referrer connection")
        it.resume(InstallReferrerResult.Unavailable)
      }
  }

  private fun readReferrer(
    client: InstallReferrerClient,
    responseCode: Int,
  ): InstallReferrerResult {
    if (responseCode != InstallReferrerClient.InstallReferrerResponse.OK) {
      Timber.w("Install referrer setup failed with response code: $responseCode")
      return InstallReferrerResult.Unavailable
    }
    return runCatching { InstallReferrerResult.Success(client.installReferrer.installReferrer) }
      .getOrElse {
        Timber.e(it, "Failed to read install referrer")
        InstallReferrerResult.Unavailable
      }
  }

  internal fun parseSurveyId(referrer: String): String? =
    referrer.split('&').firstNotNullOfOrNull { part ->
      val idx = part.indexOf('=')
      if (idx > 0 && part.substring(0, idx) == DEFERRED_SURVEY_ID_KEY) {
        part.substring(idx + 1).takeIf { it.isNotBlank() }
      } else {
        null
      }
    }

  private sealed interface InstallReferrerResult {
    data class Success(val referrer: String) : InstallReferrerResult

    data object Unavailable : InstallReferrerResult
  }

  companion object {
    private const val DEFERRED_SURVEY_ID_KEY = "survey_id"
  }
}
