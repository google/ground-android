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
import com.google.android.ground.ui.map.gms.cog.NgaCogProvider
import com.google.android.ground.ui.map.gms.cog.TileCoordinates
import java.io.File
import kotlin.math.pow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CogTest {
  @Test
  fun cogTest() {
    val tileExtentZ = 9
    val cogCollection =
      CogCollection(
        NgaCogProvider(),
        "https://storage.googleapis.com/ground-raster-basemaps/s2/2022/cog/{z}/{x}/{y}.tif",
        "https://storage.googleapis.com/ground-raster-basemaps/s2/2022/cog/world.tif",
        tileExtentZ
      )

    val basePath = File("/Users/gmiceli/Downloads/world")
    basePath.mkdirs()
    val zRange = 3..3
    for (z in zRange) {
      val f = 2.0.pow(z - 9)
      for (x in (391 * f).toInt()..(392 * f).toInt()) {
        for (y in (247 * f).toInt()..(251 * f).toInt()) {
          println("Extracting tile ($x,$y) @ zoom $z")
          val tile = cogCollection.getTile(TileCoordinates(x, y, z)) ?: continue
          File(basePath, "$z-$x-$y.jpg").writeBytes(tile.imageBytes)
        }
      }
    }
  }
}
