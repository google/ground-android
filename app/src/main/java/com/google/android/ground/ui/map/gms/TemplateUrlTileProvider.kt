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
package com.google.android.ground.ui.map.gms

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.MalformedURLException
import java.net.URL
import timber.log.Timber

/**
 * Fetches tile imagery from a server according to a formatted URL.
 *
 * `[urlTemplate]` may contain `{z}`, `{x}`, and `{y}`, which is replaced with the coordinates of
 * the tile being rendered.
 */
class TemplateUrlTileProvider(private val urlTemplate: String) : UrlTileProvider(256, 256) {
  override fun getTileUrl(x: Int, y: Int, z: Int): URL? {
    val url =
      urlTemplate
        .replace("{z}", z.toString())
        .replace("{x}", x.toString())
        .replace("{y}", y.toString())
    return try {
      URL(url)
    } catch (e: MalformedURLException) {
      Timber.e(e, "Tile URL malformed: %s", url)
      null
    }
  }
}
