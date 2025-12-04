/*
 * Copyright 2025 Google LLC
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

package org.groundplatform.android.ui.map.gms.mog

import android.util.LruCache
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.data.remote.RemoteStorageManager
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.map.Bounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MogClientTest {

  @Mock private lateinit var mockRemoteStorageManager: RemoteStorageManager
  @Mock private lateinit var mockMogCollection: MogCollection
  @Mock private lateinit var mockMogSource: MogSource
  @Mock private lateinit var mockMogMetadata: MogMetadata
  @Mock private lateinit var mockMogImageMetadata: MogImageMetadata

  private lateinit var mogClient: MogClient
  private val inputStreamFactory: (String, LongRange?) -> InputStream = { _, _ ->
    ByteArrayInputStream(ByteArray(1024) { it.toByte() })
  }

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    mogClient = MogClient(mockMogCollection, mockRemoteStorageManager, inputStreamFactory)
  }

  @Test
  fun getTile_returnsNull_whenMetadataIsNull() {
    runBlocking {
      val tileCoordinates = TileCoordinates(10, 20, 10)
      whenever(mockMogCollection.getMogSource(10)).thenReturn(mockMogSource)
      whenever(mockMogSource.getMogBoundsForTile(tileCoordinates)).thenReturn(tileCoordinates)
      whenever(mockMogSource.getMogPath(tileCoordinates)).thenReturn("path")

      // Inject null metadata into cache
      injectCache(null)

      val result = mogClient.getTile(tileCoordinates)

      assertNull(result)
    }
  }

  @Test
  fun getTile_returnsTile_whenMetadataIsPresent() {
    runBlocking {
      val tileCoordinates = TileCoordinates(10, 20, 10)
      whenever(mockMogCollection.getMogSource(10)).thenReturn(mockMogSource)
      whenever(mockMogSource.getMogBoundsForTile(tileCoordinates)).thenReturn(tileCoordinates)
      whenever(mockMogSource.getMogPath(tileCoordinates)).thenReturn("path")

      // Setup metadata
      whenever(mockMogMetadata.sourceUrl).thenReturn("url")
      whenever(mockMogMetadata.getImageMetadata(10)).thenReturn(mockMogImageMetadata)
      whenever(mockMogImageMetadata.getByteRange(10, 20)).thenReturn(0L..10L)
      whenever(mockMogImageMetadata.tileWidth).thenReturn(256)
      whenever(mockMogImageMetadata.tileLength).thenReturn(256)
      whenever(mockMogImageMetadata.jpegTables).thenReturn(ByteArray(0))
      whenever(mockMogImageMetadata.noDataValue).thenReturn(null)

      // Inject metadata into cache
      injectCache(mockMogMetadata)

      val result = mogClient.getTile(tileCoordinates)

      assertNotNull(result)
      assertEquals(tileCoordinates, result?.metadata?.tileCoordinates)
    }
  }

  @Test
  fun buildTilesRequests_returnsCorrectRequests() {
    runBlocking {
      val tileBounds = Bounds(Coordinates(0.0, 0.0), Coordinates(10.0, 10.0))
      val zoomRange = 0..0
      val tileCoordinates = TileCoordinates(0, 0, 0)

      whenever(mockMogCollection.getMogSource(0)).thenReturn(mockMogSource)
      whenever(mockMogSource.getMogBoundsForTile(tileCoordinates)).thenReturn(tileCoordinates)
      whenever(mockMogSource.getMogPath(tileCoordinates)).thenReturn("path")

      // Setup metadata
      whenever(mockMogMetadata.sourceUrl).thenReturn("url")
      whenever(mockMogMetadata.getImageMetadata(0)).thenReturn(mockMogImageMetadata)
      whenever(mockMogImageMetadata.getByteRange(0, 0)).thenReturn(0L..10L)
      whenever(mockMogImageMetadata.tileWidth).thenReturn(256)
      whenever(mockMogImageMetadata.tileLength).thenReturn(256)
      whenever(mockMogImageMetadata.jpegTables).thenReturn(ByteArray(0))
      whenever(mockMogImageMetadata.noDataValue).thenReturn(null)

      // Inject metadata into cache
      injectCache(mockMogMetadata)

      val requests = mogClient.buildTilesRequests(tileBounds, zoomRange)

      assertEquals(1, requests.size)
      assertEquals("url", requests[0].sourceUrl)
      assertEquals(1, requests[0].tiles.size)
      assertEquals(tileCoordinates, requests[0].tiles[0].tileCoordinates)
    }
  }

  @Test
  fun getTiles_returnsFlowOfTiles() {
    runBlocking {
      val tileCoordinates = TileCoordinates(0, 0, 0)
      val tileMetadata = MogTileMetadata(tileCoordinates, 256, 256, ByteArray(0), 0L..10L, null)
      val request = MogTilesRequest("url", listOf(tileMetadata))

      val flow = mogClient.getTiles(listOf(request))
      val tiles = mutableListOf<MogTile>()
      flow.collect { tiles.add(it) }

      assertEquals(1, tiles.size)
      assertEquals(tileCoordinates, tiles[0].metadata.tileCoordinates)
      assertEquals(11, tiles[0].data.size) // 0..10 is 11 bytes
    }
  }

  private fun injectCache(metadata: MogMetadata?) {
    val cacheField = MogClient::class.java.getDeclaredField("cache")
    cacheField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val cache = cacheField.get(mogClient) as LruCache<String, Deferred<MogMetadata?>>
    cache.put("path", CompletableDeferred(metadata))
  }
}
