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

package com.google.android.ground.ui.map.gms

import com.google.android.ground.ui.map.gms.cog.CogCollection
import com.google.android.ground.ui.map.gms.cog.CogTileProvider
import com.google.android.ground.ui.map.gms.cog.NgaCogProvider
import com.google.android.ground.ui.map.gms.cog.TileCoordinates
import java.io.File
import org.junit.Test

//const val URL_TEMPLATE = "/Users/gmiceli/Downloads/cogs/{z}/{x}/{y}.tif"
const val URL_TEMPLATE = "https://storage.googleapis.com/ground-raster-basemaps/s2/2022/cog/{z}/{x}/{y}.tif"

class CogTest {

  @Test
  fun cogTest() {
//    val cogCollection =
//      CogCollection(NgaCogProvider(), URL_TEMPLATE, 9)
    val cogCollection =
      CogCollection(
        NgaCogProvider(),
        "https://storage.googleapis.com/ground-raster-basemaps/s2/2022/cog/{z}/{x}/{y}.tif",
        //          requireContext().filesDir.path + "/cogs/{z}/{x}/{y}.tif",
        9
      )

    val basePath = File("/Users/gmiceli/Downloads/tiles2")
    basePath.mkdirs()
    val minZ = 9
    for (z in minZ..14) {
      val f = 1 shl (z - minZ)
      for (x in 391 * f..392 * f) {
        for (y in 247 * f..251 * f) {
          println("Extracting tile $z/$x/$y")
          val tile = cogCollection.getTile(TileCoordinates(x, y, z)) ?: continue
          File(basePath, "$z-$x-$y.jpg").writeBytes(tile.imageBytes)
        }
      }
    }
  }
}
