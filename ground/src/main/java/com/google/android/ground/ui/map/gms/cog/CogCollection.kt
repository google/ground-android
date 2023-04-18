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

import java.io.File
import timber.log.Timber

class CogCollection(private val tileSetPathTemplate: String, private val tileSetExtentsZ: Int) {
  fun getCog(x: Int, y: Int, z: Int): Cog? {
    // TODO: Implement "tileset overviews" to cover lower zoom levels.
    if (z < tileSetExtentsZ) return null
    val zDelta = z - tileSetExtentsZ
    val tileSetX = x shr zDelta
    val tileSetY = y shr zDelta
    val tileSetPath =
      tileSetPathTemplate
        .replace("{x}", tileSetX.toString())
        .replace("{y}", tileSetY.toString())
        .replace("{z}", tileSetExtentsZ.toString())
//    Timber.e("--- $x, $y, $z --> $tileSetPath")
    val tileSetFile = File(tileSetPath)
    if (!tileSetFile.exists()) return null
    return Cog(tileSetFile)
  }

  fun getTile(x: Int, y: Int, z: Int): CogTile? = getCog(x, y, z)?.getTile(x, y, z)
}
