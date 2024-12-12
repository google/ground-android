/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.ui.map.gms.mog

import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import timber.log.Timber

private const val READ_TIMEOUT_MS = 5 * 1000
private const val CONNECT_TIMEOUT_MS = 30_000

/**
 * @constructor Creates a [UrlInputStream] by opening a connection to an actual URL, requesting the
 *   specified [byteRange] if specified.
 */
class UrlInputStream(private val url: String, private val byteRange: LongRange? = null) :
  InputStream() {
  private val inputStream: InputStream

  init {
    inputStream =
      try {
        openStream()
      } catch (e: Exception) {
        Timber.e(e, "Failed to open stream for URL: $url. Using an empty stream.")
        ByteArrayInputStream(ByteArray(0))
      }
  }

  private fun openStream(): InputStream {
    val urlConnection = URL(url).openConnection() as HttpURLConnection
    return try {
      urlConnection.requestMethod = "GET"
      urlConnection.connectTimeout = CONNECT_TIMEOUT_MS
      urlConnection.readTimeout = READ_TIMEOUT_MS
      if (byteRange != null) {
        urlConnection.setRequestProperty("Range", "bytes=${byteRange.first}-${byteRange.last}")
      }
      urlConnection.connect()
      val responseCode = urlConnection.responseCode
      if (responseCode == 404) throw FileNotFoundException("$url not found")
      val expectedResponseCode = if (byteRange == null) 200 else 206
      if (responseCode != expectedResponseCode) {
        throw IOException("HTTP $responseCode accessing $url")
      }
      urlConnection.inputStream
    } catch (e: FileNotFoundException) {
      Timber.e(e, "File not found at $url. Returning an empty stream.")
      ByteArrayInputStream(ByteArray(0))
    } catch (e: IOException) {
      Timber.e(e, "I/O error while accessing $url: ${e.message}. Returning an empty stream.")
      ByteArrayInputStream(ByteArray(0))
    } catch (e: Exception) {
      Timber.e(e, "Unexpected error while accessing $url: ${e.message}. Returning an empty stream.")
      ByteArrayInputStream(ByteArray(0))
    }
  }

  override fun read(): Int = inputStream.read()

  override fun close() {
    try {
      inputStream.close()
    } catch (e: IOException) {
      Timber.e(e, "Error while closing input stream for $url")
    }
  }
}
