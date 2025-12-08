/*
 * Copyright 2025 Google LLC
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

package org.groundplatform.android.ui.map.gms.mog

import java.io.FileNotFoundException
import java.io.InputStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.groundplatform.android.common.Constants
import org.groundplatform.android.data.remote.RemoteStorageManager
import timber.log.Timber

/**
 * Loads [MogConfig] from a given URL or path.
 *
 * The JSON configuration should match the following structure:
 * ```json
 * {
 *   "sources": [
 *     {
 *       "url": "https://storage.googleapis.com/ground-raster-basemaps/s2/2022/cog/{z}/{x}/{y}.tif",
 *       "minZoom": 0,
 *       "maxZoom": 21
 *     }
 *   ]
 * }
 * ```
 */
class MogConfigLoader(
  private val remoteStorageManager: RemoteStorageManager,
  private val inputStreamFactory: (String, LongRange?) -> InputStream = { url, range ->
    UrlInputStream(url, range)
  },
) {
  private val mutex = Mutex()
  private var cachedConfig: MogConfig? = null

  /**
   * Returns the [MogConfig] for the specified [baseUrl].
   *
   * This method fetches the config from the remote source. If fetching fails, it falls back to
   * default settings defined in [Constants.getMogSources]. Results are cached to prevent redundant
   * network requests.
   */
  suspend fun getConfig(baseUrl: String): MogConfig {
    val cached = cachedConfig
    if (cached != null) return cached

    return mutex.withLock {
      cachedConfig
        ?: run {
            try {
              fetchConfig(baseUrl)
            } catch (e: Exception) {
              Timber.e(e, "Failed to load imagery config, falling back to default")
              buildDefaultConfig(baseUrl)
            }
          }
          .also { cachedConfig = it }
    }
  }

  /**
   * Fetches and parses the [MogConfig] from the specified [baseUrl].
   *
   * If [baseUrl] ends with ".json", it is treated as the full URL to the config file. Otherwise,
   * "/imagery.json" is appended to the [baseUrl].
   */
  private suspend fun fetchConfig(baseUrl: String): MogConfig {
    val configUrl = if (baseUrl.endsWith(".json")) baseUrl else "$baseUrl/imagery.json"
    val url = configUrl.toUrl() ?: throw FileNotFoundException("Invalid URL: $configUrl")
    val jsonString = inputStreamFactory(url, null).bufferedReader().use { it.readText() }
    return Json.decodeFromString<MogConfig>(jsonString)
  }

  private fun buildDefaultConfig(baseUrl: String): MogConfig =
    Constants.getMogSources(baseUrl)
      .map { MogSourceConfig(it.zoomRange.first, it.zoomRange.last, it.pathTemplate) }
      .let { MogConfig(it) }

  private suspend fun String.toUrl(): String? =
    if (startsWith("/")) {
      nullIfError { remoteStorageManager.getDownloadUrl(this).toString() }
    } else {
      this
    }

  private inline fun <T> nullIfError(fn: () -> T) =
    try {
      fn()
    } catch (e: Exception) {
      Timber.e(e)
      null
    }
}
