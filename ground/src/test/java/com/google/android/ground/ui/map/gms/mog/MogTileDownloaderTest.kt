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
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MogTileDownloaderTest {
  @Test
  fun downloadTiles() = runBlocking {
    // Indonesia: LngLatBbox(west=95.00933042066814, south=-11.007616494507769,
    // east=141.01944540877238, north=5.904411619513165)
    val southwest = LatLng(-11.007616494507769, 95.00933042066814)
    val northeast = LatLng(5.904411619513165, 141.01944540877238)
    //    val southwest = LatLng(4.089672, 95.546853)
    //    val northeast = LatLng(5.435577, 96.278013)
    val mogBaseUrl = "https://storage.googleapis.com/ground-raster-basemaps/2022/cog/8"
    val mogCollection = MogCollection("$mogBaseUrl/world.tif", "$mogBaseUrl/{x}/{y}.tif", 8, 14)
    val downloader = MogTileDownloader(mogCollection, "/tmp/tiles")
    var startTimeMillis = System.currentTimeMillis()
    println("Fetching headers...")
    downloader.downloadTiles(LatLngBounds(southwest, northeast), 12..14)
  }
}
