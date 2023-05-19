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

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.lang.System.currentTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.ConscryptMode.Mode.OFF

@RunWith(RobolectricTestRunner::class)
@ConscryptMode(OFF)
class MogTileDownloaderTest {
  @Test
  fun downloadTiles() = runBlocking {
    // Indonesia: LngLatBbox(west=95.00933042066814, south=-11.007616494507769,
    // east=141.01944540877238, north=5.904411619513165)
    val allOfIndonesia =
      LatLngBounds(
        LatLng(-11.007616494507769, 95.00933042066814),
        LatLng(5.904411619513165, 141.01944540877238)
      )
    val acehProvince = LatLngBounds(LatLng(2.032045, 95.031738), LatLng(5.954827, 98.421021))
    //    val southwest = LatLng(4.089672, 95.546853)
    //    val northeast = LatLng(5.435577, 96.278013)
    val mogBaseUrl = "https://storage.googleapis.com/ground-raster-basemaps/2022/cog/{z}"
    val mogCollection = MogCollection("$mogBaseUrl/world.tif", "$mogBaseUrl/{x}/{y}.tif", 8, 14)
    val client = MogClient(mogCollection)
    val downloader = MogTileDownloader(client, "/tmp/tiles")

    println("Fetching headers...")
    val startTimeMillis = currentTimeMillis()
    val requests = client.getTilesRequests(acehProvince)
    val timeSecs = (currentTimeMillis() - startTimeMillis) / 1000.0
    val numRequests = requests.size
    val numFiles = requests.map { it.sourceUrl }.distinct().size
    val numTiles = requests.sumOf { it.tiles.sumOf { tile -> tile.byteRange.count() } }
    println("Headers in %.1f".format(timeSecs))
    println("# requests: $numRequests")
    println("# files:    $numFiles")
    println("# tiles:    $numTiles")

//    downloader.downloadTiles(requests)
  }
}
