/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.repository

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.data.local.stores.LocalOfflineAreaStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.system.GeocodingManager
import org.groundplatform.android.ui.map.gms.mog.MogClient
import org.groundplatform.android.ui.map.gms.mog.MogCollection
import org.groundplatform.android.ui.map.gms.mog.MogSource
import org.groundplatform.android.ui.map.gms.mog.MogTilesRequest
import org.groundplatform.android.ui.map.gms.mog.getTilePath
import org.groundplatform.android.ui.util.FileUtil
import org.groundplatform.domain.model.imagery.LocalTileSource
import org.groundplatform.domain.model.imagery.OfflineArea
import org.groundplatform.domain.model.map.Bounds
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class OfflineAreaRepositoryTest {

  @get:Rule val tempFolder: TemporaryFolder = TemporaryFolder()

  @Mock private lateinit var localOfflineAreaStore: LocalOfflineAreaStore
  @Mock private lateinit var fileUtil: FileUtil
  @Mock private lateinit var geocodingManager: GeocodingManager
  @Mock private lateinit var offlineUuidGenerator: OfflineUuidGenerator

  private lateinit var mogClient: MogClient
  private lateinit var repository: OfflineAreaRepository

  @Before
  fun setUp() {
    whenever(fileUtil.getFilesDir()).thenReturn(tempFolder.root)
    mogClient = spy(MogClient(TEST_COLLECTION, mock()))
    repository =
      OfflineAreaRepository(
        localOfflineAreaStore,
        fileUtil,
        geocodingManager,
        mogClient,
        offlineUuidGenerator,
      )
  }

  @Test
  fun `offlineAreas gets all offline areas from the local store`() = runTest {
    val areas = listOf(TEST_AREA)
    whenever(localOfflineAreaStore.offlineAreas()).thenReturn(flowOf(areas))

    assertThat(repository.offlineAreas().first()).isEqualTo(areas)
  }

  @Test
  fun `getOfflineArea gets a specific offline area from the local store`() = runTest {
    whenever(localOfflineAreaStore.getOfflineAreaById("id")).thenReturn(TEST_AREA)

    assertThat(repository.getOfflineArea("id")).isEqualTo(TEST_AREA)
  }

  @Test
  fun `getOfflineArea returns null when area not found`() = runTest {
    whenever(localOfflineAreaStore.getOfflineAreaById("missing")).thenReturn(null)

    assertThat(repository.getOfflineArea("missing")).isNull()
  }

  @Test
  fun `downloadTiles emits no progress when there are no tile requests`() = runTest {
    doAnswer { emptyList<MogTilesRequest>() }.whenever(mogClient).buildTilesRequests(any(), any())

    val progress = repository.downloadTiles(TEST_BOUNDS).toList()

    assertThat(progress).isEmpty()
  }

  @Test
  fun `downloadTiles does not save offline area when no tiles are downloaded`() = runTest {
    doAnswer { emptyList<MogTilesRequest>() }.whenever(mogClient).buildTilesRequests(any(), any())

    repository.downloadTiles(TEST_BOUNDS).toList()

    verifyBlocking(localOfflineAreaStore, never()) { insertOrUpdate(any()) }
    verifyBlocking(geocodingManager, never()) { getAreaName(any()) }
  }

  @Test
  fun `getOfflineTileSourcesFlow emits LocalTileSource when areas exist`() = runTest {
    whenever(localOfflineAreaStore.offlineAreas()).thenReturn(flowOf(listOf(TEST_AREA)))

    val source = repository.getOfflineTileSourcesFlow().first()

    with(source as LocalTileSource) {
      assertThat(maxZoom).isEqualTo(TEST_AREA.zoomRange.last)
      assertThat(clipBounds).containsExactly(TEST_AREA.bounds)
      assertThat(localFilePath).endsWith("/tiles/{z}/{x}/{y}.jpg")
    }
  }

  @Test
  fun `getOfflineTileSourcesFlow emits nothing when no areas exist`() = runTest {
    whenever(localOfflineAreaStore.offlineAreas()).thenReturn(flowOf(emptyList()))

    assertThat(repository.getOfflineTileSourcesFlow().toList()).isEmpty()
  }

  @Test
  fun `hasHiResImagery returns true when tiles available`() = runTest {
    val request = mock<MogTilesRequest>()
    doAnswer { listOf(request) }.whenever(mogClient).buildTilesRequests(any(), any())

    val result = repository.hasHiResImagery(TEST_BOUNDS)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrNull()).isTrue()
  }

  @Test
  fun `hasHiResImagery returns false when no tiles`() = runTest {
    doAnswer { emptyList<MogTilesRequest>() }.whenever(mogClient).buildTilesRequests(any(), any())

    val result = repository.hasHiResImagery(TEST_BOUNDS)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrNull()).isFalse()
  }

  @Test
  fun `hasHiResImagery returns failure on IOException`() = runTest {
    val exception = IOException("network down")
    doAnswer { throw exception }.whenever(mogClient).buildTilesRequests(any(), any())

    val result = repository.hasHiResImagery(TEST_BOUNDS)

    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isEqualTo(exception)
  }

  @Test
  fun `estimateSizeOnDisk sums total bytes across requests`() = runTest {
    val r1 = mock<MogTilesRequest> { on { totalBytes } doReturn 100 }
    val r2 = mock<MogTilesRequest> { on { totalBytes } doReturn 250 }
    doAnswer { listOf(r1, r2) }.whenever(mogClient).buildTilesRequests(any(), any())

    val result = repository.estimateSizeOnDisk(TEST_BOUNDS)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrNull()).isEqualTo(350)
  }

  @Test
  fun `estimateSizeOnDisk returns failure on IOException`() = runTest {
    val exception = IOException("network down")
    doAnswer { throw exception }.whenever(mogClient).buildTilesRequests(any(), any())

    val result = repository.estimateSizeOnDisk(TEST_BOUNDS)

    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isEqualTo(exception)
  }

  @Test
  fun `sizeOnDevice returns zero when no tile files exist`() {
    val area = OfflineArea("id_small", OfflineArea.State.DOWNLOADED, TEST_BOUNDS, "Small", 0..0)

    assertThat(repository.sizeOnDevice(area)).isEqualTo(0)
  }

  @Test
  fun `sizeOnDevice returns sum of tile file lengths`() {
    val area = OfflineArea("id_small", OfflineArea.State.DOWNLOADED, TEST_BOUNDS, "Small", 0..0)
    val tilesDir = File(tempFolder.root, "tiles")
    val contents = "abcde"
    area.tiles.forEach { coords ->
      File(tilesDir, coords.getTilePath()).apply {
        parentFile?.mkdirs()
        writeText(contents)
      }
    }
    val expectedSize = area.tiles.size * contents.length

    assertThat(repository.sizeOnDevice(area)).isEqualTo(expectedSize)
  }

  @Test
  fun `removeFromDevice deletes area from local store, tile files and their empty parent directories`() =
    runTest {
      val area =
        OfflineArea(TEST_AREA.id, OfflineArea.State.DOWNLOADED, TEST_BOUNDS, "Small", 0..10)
      val tilesDir = File(tempFolder.root, "tiles")
      val tileFiles =
        area.tiles.map { coords ->
          File(tilesDir, coords.getTilePath()).apply {
            parentFile?.mkdirs()
            writeText("tile")
          }
        }
      val zoomDirs = tileFiles.map { it.parentFile!!.parentFile!! }.toSet()
      whenever(localOfflineAreaStore.offlineAreas()).thenReturn(flowOf(emptyList()))

      repository.removeFromDevice(area)

      verify(localOfflineAreaStore).deleteOfflineArea(TEST_AREA.id)
      tileFiles.forEach { assertThat(it.exists()).isFalse() }
      tileFiles.map { it.parentFile!! }.toSet().forEach { assertThat(it.exists()).isFalse() }
      zoomDirs.forEach { assertThat(it.exists()).isFalse() }
    }

  @Test
  fun `removeAllOfflineAreas deletes all areas and removes tile directory`() = runTest {
    val tilesDir = File(tempFolder.root, "tiles")
    tilesDir.mkdirs()
    whenever(localOfflineAreaStore.offlineAreas()).thenReturn(flowOf(emptyList()))

    repository.removeAllOfflineAreas()

    assertThat(tilesDir.exists()).isFalse()
  }

  @Test
  fun `removeAllOfflineAreas succeeds when tile directory is absent`() = runTest {
    whenever(localOfflineAreaStore.offlineAreas()).thenReturn(flowOf(emptyList()))

    repository.removeAllOfflineAreas()

    assertThat(File(tempFolder.root, "tiles").exists()).isFalse()
  }

  private companion object {
    val TEST_BOUNDS = Bounds(0.0, 0.0, 1.0, 1.0)
    val TEST_AREA =
      OfflineArea("id", OfflineArea.State.DOWNLOADED, TEST_BOUNDS, "Test Area", 0..14)
    val TEST_COLLECTION = MogCollection(listOf(MogSource(0..14, "/path/{z}/{x}/{y}.tif")))
  }
}
