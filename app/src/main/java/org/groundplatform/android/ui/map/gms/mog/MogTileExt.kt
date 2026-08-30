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

package org.groundplatform.android.ui.map.gms.mog

import android.graphics.Color
import com.google.android.gms.maps.model.Tile
import org.groundplatform.android.util.image.TileImageTransformer
import org.groundplatform.domain.model.imagery.MogTile

/** Returns a Maps SDK [Tile] instance for this [MogTile]. */
fun MogTile.toGmsTile(): Tile = Tile(metadata.width, metadata.height, getProcessedImageData())

/**
 * Returns the JFIF image data for this tile, with any pixels matching [metadata.noDataValue] masked
 * as fully transparent.
 */
fun MogTile.getProcessedImageData(): ByteArray {
  val noData = metadata.noDataValue ?: return buildJfifFile()
  val noDataColor = Color.rgb(noData, noData, noData)
  return TileImageTransformer.setTransparentIf(buildJfifFile()) { bitmap, x, y ->
    bitmap.getPixel(x, y) == noDataColor
  }
}
