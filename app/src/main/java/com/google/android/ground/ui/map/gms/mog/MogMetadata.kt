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

import kotlinx.coroutines.flow.*

/**
 * Contiguous tiles are fetched in a single request. To minimize the number of server requests, we
 * also allow additional unneeded tiles to be fetched so that nearly contiguous tiles can be also
 * fetched in a single request. This constant defines the maximum number of unneeded bytes which can
 * be fetched per tile to allow nearly contiguous regions to be merged. Tiles are typically 10-20
 * KB, so allowing 20 KB over fetch generally allows 1-2 extra tiles to be fetched.
 */
const val MAX_OVER_FETCH_PER_TILE = 1 * 20 * 1024

/**
 * A single Maps Optimized GeoTIFF (MOG). MOGs are [Cloud Optimized GeoTIFFs (COGs)](cogeo.org)
 * clipped and configured for visualization with Google Maps Platform. This class stores metadata
 * and fetches tiles on demand via [getTile] and [getTiles].
 */
data class MogMetadata(
  /** The URL of the source MOG. */
  val sourceUrl: String,
  /** The web mercator tile coordinates corresponding to the bounding box of the source MOG. */
  val bounds: TileCoordinates,
  val imageMetadata: List<MogImageMetadata>,
) {
  private val imageMetadataByZoom = imageMetadata.associateBy { it.zoom }

  fun getImageMetadata(zoom: Int): MogImageMetadata? = imageMetadataByZoom[zoom]

  override fun toString(): String =
    "Mog(url=$sourceUrl, bounds=$bounds, ifdsByZoom=$imageMetadataByZoom)"
}
