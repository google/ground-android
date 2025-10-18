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

package org.groundplatform.android.ui.map.gms

import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.TileProvider.NO_TILE
import org.groundplatform.android.ui.map.gms.mog.ImageEditor
import org.groundplatform.android.ui.map.gms.mog.TileCoordinates
import org.groundplatform.android.ui.map.gms.mog.pixelToLatLng

class ClippingTileProvider(
  private val sourceTileProvider: TileProvider,
  private val clipBounds: List<LatLngBounds>,
) : TileProvider {

  override fun getTile(x: Int, y: Int, zoom: Int): Tile {
    val sourceTile = sourceTileProvider.getTile(x, y, zoom) ?: NO_TILE
    val data = sourceTile.data ?: return NO_TILE

    val coords = TileCoordinates(x, y, zoom)
    val output =
      ImageEditor.setTransparentIf(data) { _, x, y ->
        val latLng = coords.pixelToLatLng(x, y)
        val insideAny = clipBounds.any { it.contains(latLng) }
        !insideAny // make transparent if NOT inside any bound
      }
    return Tile(sourceTile.width, sourceTile.height, output)
  }
}
