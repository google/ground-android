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

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.ui.map.gms.cog.CogCollection
import com.google.android.ground.ui.map.gms.cog.HttpCogSource
import com.google.android.ground.ui.map.gms.cog.NgaCogHeaderParser
import java.io.File
import kotlinx.coroutines.runBlocking
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
        NgaCogHeaderParser(),
        HttpCogSource(),
        "https://storage.googleapis.com/ground-raster-basemaps/s2/2022/cog/{z}/{x}/{y}.tif",
        "https://storage.googleapis.com/ground-raster-basemaps/s2/2022/cog/world.tif",
        tileExtentZ
      )

    val outpath = File("/Users/gmiceli/Downloads/tiles")
    outpath.mkdirs()
    //    val tileExtentsZ = 9
    //    val zRange = 9..9
    //    val xRange = 391..392
    //    val yRange = 247..251
    val southwest = LatLng(4.089672, 95.546853)
    val northeast = LatLng(5.435577, 96.278013)
    runBlocking {
      cogCollection.getTiles(LatLngBounds(southwest, northeast), 9..10).collect {
        it.fold(
          { tile ->
            println("Saving tile ${tile.coordinates}")
            val (x, y, zoom) = tile.coordinates
            File(outpath, "$zoom-$x-$y.jpg").writeBytes(tile.imageBytes)
          },
          { error -> println("Failure: $error") }
        )
      }
    }
    //    for (z in zRange) {
    //      val f = 2.0.pow(z - tileExtentsZ)
    //      for (x in xRange.map { (it * f).toInt() }) {
    //        for (y in yRange.map { (it * f).toInt() }) {
    //          println("Extracting tile ($x,$y) @ zoom $z")
    //          val tile = cogCollection.getTile(TileCoordinates(x, y, z)) ?: continue
    //          File(basePath, "$z-$x-$y.jpg").writeBytes(tile.imageBytes)
    //        }
    //      }
    //    }
  }
}
