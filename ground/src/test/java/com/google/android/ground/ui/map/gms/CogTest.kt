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
import com.google.android.ground.ui.map.gms.cog.CogTileDownloader
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
    val cogCollection =
      CogCollection(
        NgaCogHeaderParser(),
        HttpCogSource(),
        "https://storage.googleapis.com/ground-raster-basemaps/s2/2022/cog/{z}/{x}/{y}.tif",
        "https://storage.googleapis.com/ground-raster-basemaps/s2/2022/cog/world.tif",
        9,
        10
      )
    val southwest = LatLng(4.089672, 95.546853)
    val northeast = LatLng(5.435577, 96.278013)
    val downloader = CogTileDownloader(cogCollection, "/tmp/tiles")
    runBlocking {
      downloader.downloadTiles(LatLngBounds(southwest, northeast))
    }
  }

  @Test
  fun mergeRanges() {
    val ranges = listOf(LongRange(0, 3), LongRange(4, 7), LongRange(9, 11), LongRange(13, 15))

    val result = mutableListOf<LongRange>()
    for (range in ranges) {
      if (result.isEmpty() || range.first - result.last().last - 1 > 0) {
        result.add(range)
      } else {
        result[result.lastIndex] = LongRange(result.last().first, range.last)
      }
    }

    println(result)
  }
}
