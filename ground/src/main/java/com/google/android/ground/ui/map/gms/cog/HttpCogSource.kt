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

package com.google.android.ground.ui.map.gms.cog

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

const val READ_TIMEOUT_MS = 5 * 1000

class HttpCogSource : CogSource {

  override fun openStream(url: String, ranges: Iterable<LongRange>?): InputStream? {
    val urlConnection = URL(url).openConnection() as HttpURLConnection
    urlConnection.requestMethod = "GET"
    urlConnection.readTimeout = READ_TIMEOUT_MS
    if (ranges != null) {
      val bytesList = ranges.joinToString(",") { "${it.first}-${it.last}" }
      urlConnection.setRequestProperty("Range", "bytes=$bytesList")
    }
    urlConnection.connect()
    val responseCode = urlConnection.responseCode
    if (responseCode == 404) return null
    val expectedResponseCode = if (ranges == null) 200 else 206
    if (responseCode != expectedResponseCode) {
      throw CogException("HTTP $responseCode accessing $url")
    }
    return urlConnection.inputStream
  }
}
