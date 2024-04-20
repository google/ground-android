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
package com.google.android.ground.repository

import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.persistence.local.stores.LocalOfflineAreaStore
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.system.GeocodingManager
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.mog.MogClient
import com.google.android.ground.ui.map.gms.mog.MogTileDownloader
import com.google.android.ground.ui.map.gms.mog.getTilePath
import com.google.android.ground.ui.map.gms.mog.maxZoom
import com.google.android.ground.ui.util.FileUtil
import com.google.android.ground.util.ByteCount
import com.google.android.ground.util.deleteIfEmpty
import com.google.android.ground.util.rangeOf
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
  private val tileSources: List<TileSource>,
  private val localOfflineAreaStore: LocalOfflineAreaStore,
  private val surveyRepository: SurveyRepository,
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
  suspend fun downloadTiles(bounds: Bounds): Flow<Pair<Int, Int>> = flow {
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

  // TODO(#1730): Generate local tiles path based on source base path.
  private fun getLocalTileSourcePath(): String = File(fileUtil.getFilesDir(), "tiles").path

  fun getOfflineTileSourcesFlow() =
    surveyRepository.activeSurveyFlow.combine(getOfflineAreaBounds()) { _, bounds ->
      applyBounds(tileSources, bounds)
    }

  private fun applyBounds(tileSources: List<TileSource>?, bounds: List<Bounds>): List<TileSource> =
    tileSources?.mapNotNull { tileSource -> toOfflineTileSource(tileSource, bounds) } ?: listOf()

  private fun toOfflineTileSource(tileSource: TileSource, clipBounds: List<Bounds>): TileSource? {
    if (tileSource.type != TileSource.Type.MOG_COLLECTION) return null
    return TileSource(
      "file://${getLocalTileSourcePath()}/{z}/{x}/{y}.jpg",
      TileSource.Type.TILED_WEB_MAP,
      clipBounds,
    )
  }

  private fun getOfflineAreaBounds(): Flow<List<Bounds>> =
    localOfflineAreaStore.offlineAreas().map { list -> list.map { it.bounds } }

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
}
