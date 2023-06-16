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

/** A collection of Maps Optimized GeoTIFFs (MOGs). */
class MogCollection(
  private val worldMogUrl: String,
  private val hiResMogUrl: String,
  val hiResMogMinZoom: Int,
  val hiResMogMaxZoom: Int
) {
  fun getMogUrl(bounds: TileCoordinates): String {
    val (x, y, zoom) = bounds
    if (zoom == 0) {
      worldMogUrl.checkContainsAll("{z}")
      return worldMogUrl.replace("{z}", hiResMogMinZoom.toString())
    }
    if (zoom < hiResMogMinZoom) {
      error("Invalid zoom for this collection. Expected 0 or $hiResMogMinZoom, got $zoom")
    }
    hiResMogUrl.checkContainsAll("{x}", "{y}", "{z}")
    return hiResMogUrl
      .replace("{x}", x.toString())
      .replace("{y}", y.toString())
      .replace("{z}", hiResMogMinZoom.toString())
  }

  /** Returns the bounds of the MOG containing the tile with the specified coordinates. */
  fun getMogBoundsForTile(tileCoordinates: TileCoordinates): TileCoordinates =
    if (tileCoordinates.zoom < hiResMogMinZoom) TileCoordinates.WORLD
    else tileCoordinates.originAtZoom(hiResMogMinZoom)

  private fun String.checkContainsAll(vararg substrings: String) {
    substrings.forEach { param -> check(this.contains(param)) { "$this doesn't contain $param" } }
  }
}
