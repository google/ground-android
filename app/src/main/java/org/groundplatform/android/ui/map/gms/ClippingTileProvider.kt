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

/**
 * A [TileProvider] that clips tiles to one or more geographic rectangles.
 *
 * Pixels outside the provided [clipBounds] are made transparent. This is implemented by delegating
 * to [sourceTileProvider] and post-processing the returned PNG/bitmap bytes.
 *
 * Note: We short-circuit when the tile is fully outside (return [TileProvider.NO_TILE]) or fully
 * inside (return the source tile unchanged) to avoid per-pixel work.
 */
class ClippingTileProvider(
  private val sourceTileProvider: TileProvider,
  private val clipBounds: List<LatLngBounds>,
) : TileProvider {

  override fun getTile(x: Int, y: Int, zoom: Int): Tile {
    val sourceTile = sourceTileProvider.getTile(x, y, zoom) ?: return NO_TILE
    val bytes = sourceTile.data ?: return NO_TILE

    val coords = TileCoordinates(x, y, zoom)
    val tileLatLngBounds = coords.latLngBounds()

    val resultTile: Tile =
      when {
        // Fully outside every clip → nothing to draw
        clipBounds.none { it.intersects(tileLatLngBounds) } -> {
          NO_TILE
        }

        // Fully contained → use as-is
        clipBounds.any { it.contains(tileLatLngBounds) } -> {
          sourceTile
        }

        // Partial overlap → mask outside pixels
        else -> {
          val masked =
            ImageEditor.setTransparentIf(bytes) { _, px, py ->
              val latLng = coords.pixelToLatLng(px, py)
              val insideAny = clipBounds.any { it.contains(latLng) }
              !insideAny // transparent if outside all bounds
            }
          Tile(sourceTile.width, sourceTile.height, masked)
        }
      }

    return resultTile
  }

  /** Utility: compute the LatLng bounds of a whole 256×256 (or provider-sized) tile. */
  private fun TileCoordinates.latLngBounds(tileSize: Int = 256): LatLngBounds {
    // top-left pixel (0,0) and bottom-right pixel (tileSize-1, tileSize-1)
    val nw = pixelToLatLng(0, 0)
    val se = pixelToLatLng(tileSize - 1, tileSize - 1)
    return LatLngBounds(nw, se)
  }

  /** Returns true if this bounds fully contains the other bounds (non-strict). */
  private fun LatLngBounds.contains(other: LatLngBounds) =
    this.contains(other.northeast) && this.contains(other.southwest)

  /** Returns true if this bounds intersects the other bounds (AABB test). */
  private fun LatLngBounds.intersects(other: LatLngBounds): Boolean {
    // Simple rectangle intersection in Lat/Lng; ensure your LatLngBounds handles antimeridian if
    // needed.
    val noOverlap =
      other.southwest.latitude > this.northeast.latitude ||
        other.northeast.latitude < this.southwest.latitude ||
        other.southwest.longitude > this.northeast.longitude ||
        other.northeast.longitude < this.southwest.longitude
    return !noOverlap
  }
}
