/*
 * Copyright 2020 Google LLC
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

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import org.groundplatform.android.data.local.stores.LocalOfflineAreaStore
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.android.model.imagery.LocalTileSource
import org.groundplatform.android.model.imagery.OfflineArea
import org.groundplatform.android.model.imagery.TileSource
import org.groundplatform.android.model.map.Bounds
import org.groundplatform.android.system.GeocodingManager
import org.groundplatform.android.ui.map.gms.mog.MogClient
import org.groundplatform.android.ui.map.gms.mog.MogTileDownloader
import org.groundplatform.android.ui.map.gms.mog.getTilePath
import org.groundplatform.android.ui.map.gms.mog.maxZoom
import org.groundplatform.android.ui.util.FileUtil
import org.groundplatform.android.util.ByteCount
import org.groundplatform.android.util.deleteIfEmpty
import org.groundplatform.android.util.rangeOf
import timber.log.Timber

/**
 * Corners of the viewport are scaled by this value when determining the name of downloaded areas.
 * Value derived experimentally.
 */
const val AREA_NAME_SENSITIVITY = 0.5

@Singleton
class OfflineAreaRepository
@Inject
constructor(
  private val localOfflineAreaStore: LocalOfflineAreaStore,
  private val fileUtil: FileUtil,
  private val geocodingManager: GeocodingManager,
  private val mogClient: MogClient,
  private val offlineUuidGenerator: OfflineUuidGenerator,
) {

  private suspend fun addOfflineArea(bounds: Bounds, zoomRange: IntRange) {
    val areaName = geocodingManager.getAreaName(bounds.shrink(AREA_NAME_SENSITIVITY))
    localOfflineAreaStore.insertOrUpdate(
      OfflineArea(
        offlineUuidGenerator.generateUuid(),
        OfflineArea.State.DOWNLOADED,
        bounds,
        areaName,
        zoomRange,
      )
    )
  }

  /**
   * Retrieves all offline areas from the local store and continually streams the list as the local
   * store is updated.
   */
  fun offlineAreas(): Flow<List<OfflineArea>> = localOfflineAreaStore.offlineAreas()

  /** Fetches a single offline area by ID. */
  suspend fun getOfflineArea(offlineAreaId: String): OfflineArea? =
    localOfflineAreaStore.getOfflineAreaById(offlineAreaId)

  /**
   * Downloads tiles in the specified bounds and stores them in the local filesystem. Emits the
   * number of bytes processed and total expected bytes as the download progresses.
   */
  fun downloadTiles(bounds: Bounds): Flow<Pair<Int, Int>> = flow {
    val requests = mogClient.buildTilesRequests(bounds)
    val totalBytes = requests.sumOf { it.totalBytes }
    var bytesDownloaded = 0
    val tilePath = getLocalTileSourcePath()
    MogTileDownloader(mogClient, tilePath).downloadTiles(requests).collect {
      bytesDownloaded += it
      emit(Pair(bytesDownloaded, totalBytes))
    }
    if (bytesDownloaded > 0) {
      val zoomRange = requests.flatMap { it.tiles }.rangeOf { it.tileCoordinates.zoom }
      addOfflineArea(bounds, zoomRange)
    }
  }

  // TODO: Generate local tiles path based on source base path.
  // Issue URL: https://github.com/google/ground-android/issues/1730
  private fun getLocalTileDirectory(): File = File(fileUtil.getFilesDir(), "tiles")

  private fun getLocalTileSourcePath(): String = getLocalTileDirectory().path

  fun getOfflineTileSourcesFlow(): Flow<TileSource> =
    localOfflineAreaStore.offlineAreas().mapNotNull(::mapOfflineAreasToTileSource)

  private fun mapOfflineAreasToTileSource(list: List<OfflineArea>): TileSource? {
    if (list.isEmpty()) return null
    val maxZoom = list.maxOfOrNull { it.zoomRange.last } ?: return null
    val bounds = list.map { it.bounds }

    return LocalTileSource(
      localFilePath = "file://${getLocalTileSourcePath()}/{z}/{x}/{y}.jpg",
      clipBounds = bounds,
      maxZoom = maxZoom,
    )
  }

  suspend fun hasHiResImagery(bounds: Bounds): Boolean {
    val maxZoom = mogClient.collection.sources.maxZoom()
    return mogClient.buildTilesRequests(bounds, maxZoom..maxZoom).isNotEmpty()
  }

  suspend fun estimateSizeOnDisk(bounds: Bounds): Int {
    val requests = mogClient.buildTilesRequests(bounds)
    return requests.sumOf { it.totalBytes }
  }

  /** Returns the number of bytes occupied by tiles on the local device. */
  fun sizeOnDevice(offlineArea: OfflineArea): ByteCount =
    offlineArea.tiles.sumOf { File(getLocalTileSourcePath(), it.getTilePath()).length().toInt() }

  /**
   * Deletes the provided offline area from the device, including all associated unused tiles on the
   * local filesystem. Folders containing the deleted tiles are also removed if empty.
   */
  suspend fun removeFromDevice(offlineArea: OfflineArea) {
    val tilesInSelectedArea = offlineArea.tiles
    if (tilesInSelectedArea.isEmpty()) Timber.w("No tiles associate with offline area $offlineArea")
    localOfflineAreaStore.deleteOfflineArea(offlineArea.id)
    val remainingAreas = localOfflineAreaStore.offlineAreas().first()
    val remainingTiles = remainingAreas.flatMap { it.tiles }.toSet()
    val tilesToRemove = tilesInSelectedArea - remainingTiles
    val tileSourcePath = getLocalTileSourcePath()
    tilesToRemove.forEach {
      with(File(tileSourcePath, it.getTilePath())) {
        delete()
        parentFile?.deleteIfEmpty()
        parentFile?.parentFile?.deleteIfEmpty()
      }
    }
  }

  suspend fun removeAllOfflineAreas() {
    localOfflineAreaStore.offlineAreas().first().forEach { removeFromDevice(it) }
    val directoryToDelete = getLocalTileDirectory()
    if (directoryToDelete.exists()) {
      val success = directoryToDelete.deleteRecursively()
      if (success) {
        Timber.d("Deleted directory: $directoryToDelete")
      } else {
        Timber.e("Failed to delete directory: $directoryToDelete")
      }
    }
  }
}
