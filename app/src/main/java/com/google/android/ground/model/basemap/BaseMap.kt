/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.model.basemap

import java.net.URL
import org.apache.commons.io.FilenameUtils

/** Represents a possible source for offline base map data. */
data class BaseMap(val url: URL, val type: BaseMapType) {
  enum class BaseMapType {
    MBTILES_FOOTPRINTS,
    TILED_WEB_MAP,
    UNKNOWN
  }

  companion object {
    fun typeFromExtension(url: String): BaseMapType =
      when (FilenameUtils.getExtension(url)) {
        "geojson" -> BaseMapType.MBTILES_FOOTPRINTS
        "png" -> BaseMapType.TILED_WEB_MAP
        else -> BaseMapType.UNKNOWN
      }
  }
}
