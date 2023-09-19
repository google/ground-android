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

import com.google.android.ground.Config
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.persistence.local.stores.LocalOfflineAreaStore
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.GeocodingManager
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.mog.*
import com.google.android.ground.ui.map.gms.toGoogleMapsObject
import com.google.android.ground.ui.util.FileUtil
import io.reactivex.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow

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
  private val surveyRepository: SurveyRepository,
  private val fileUtil: FileUtil,
  private val geocodingManager: GeocodingManager,
  private val offlineUuidGenerator: OfflineUuidGenerator
) {

  private suspend fun addOfflineArea(bounds: Bounds) {
    val areaName = geocodingManager.getAreaName(bounds.shrink(AREA_NAME_SENSITIVITY))
    localOfflineAreaStore.insertOrUpdate(
      OfflineArea(
        offlineUuidGenerator.generateUuid(),
        OfflineArea.State.DOWNLOADED,
        bounds,
        areaName
      )
    )
  }

  /**
   * Retrieves all offline areas from the local store and continually streams the list as the local
   * store is updated. Triggers `onError` only if there is a problem accessing the local store.
   */
  fun offlineAreasOnceAndStream(): @Cold(terminates = false) Flowable<List<OfflineArea>> =
    localOfflineAreaStore.offlineAreasOnceAndStream()

  /**
   * Fetches a single offline area by ID. Triggers `onError` when the area is not found. Triggers
   * `onSuccess` when the area is found.
   */
  fun getOfflineArea(offlineAreaId: String): @Cold Single<OfflineArea> =
    localOfflineAreaStore.getOfflineAreaById(offlineAreaId)

  /**
   * Downloads tiles in the specified bounds and stores them in the local filesystem. Emits the
   * number of bytes processed and total expected bytes as the download progresses.
   */
  suspend fun downloadTiles(bounds: Bounds): Flow<Pair<Int, Int>> = flow {
    val client = getMogClient()
    val requests = client.buildTilesRequests(bounds.toGoogleMapsObject())
    val totalBytes = requests.sumOf { it.totalBytes }
    var bytesDownloaded = 0
    val tilePath = getLocalTileSourcePath()
    MogTileDownloader(client, tilePath).downloadTiles(requests).collect {
      bytesDownloaded += it
      emit(Pair(bytesDownloaded, totalBytes))
    }
    if (bytesDownloaded > 0) {
      addOfflineArea(bounds)
    }
  }

  // TODO(#1730): Generate local tiles path based on source base path.
  private suspend fun getLocalTileSourcePath(): String = File(fileUtil.getFilesDir(), "tiles").path

  fun getOfflineTileSourcesFlow() =
    surveyRepository.activeSurveyFlow
      // TODO(#1593): Use Room DAO's Flow once we figure out why it never emits a value.
      .combine(getOfflineAreaBounds().asFlow()) { survey, bounds ->
        applyBounds(survey?.tileSources, bounds)
      }

  private suspend fun applyBounds(
    tileSources: List<TileSource>?,
    bounds: List<Bounds>
  ): List<TileSource> =
    tileSources?.mapNotNull { tileSource -> toOfflineTileSource(tileSource, bounds) } ?: listOf()

  private suspend fun toOfflineTileSource(
    tileSource: TileSource,
    clipBounds: List<Bounds>
  ): TileSource? {
    if (tileSource.type != TileSource.Type.MOG_COLLECTION) return null
    return TileSource(
      "file://${getLocalTileSourcePath()}/{z}/{x}/{y}.jpg",
      TileSource.Type.TILED_WEB_MAP,
      clipBounds
    )
  }

  private fun getOfflineAreaBounds(): Flowable<List<Bounds>> =
    localOfflineAreaStore.offlineAreasOnceAndStream().map { list -> list.map { it.bounds } }

  /**
   * Uses the first tile source URL of the currently active survey and returns a [MogClient], or
   * throws an error if no survey is active or if no tile sources are defined.
   */
  private fun getMogClient(): MogClient {
    val baseUrl = getFirstTileSourceUrl()
    val mogCollection = MogCollection(Config.getMogSources(baseUrl))
    // TODO(#1754): Create a factory and inject rather than instantiating here. Add tests.
    return MogClient(mogCollection)
  }

  /**
   * Returns the URL of the first tile source in the current survey, or throws an error if no survey
   * is active or if no tile sources are defined.
   */
  private fun getFirstTileSourceUrl() =
    surveyRepository.activeSurvey?.tileSources?.firstOrNull()?.url
      ?: error("Survey has no tile sources")

  suspend fun hasHiResImagery(bounds: Bounds): Boolean {
    val client = getMogClient()
    val maxZoom = client.collection.maxZoom
    return client.buildTilesRequests(bounds.toGoogleMapsObject(), maxZoom..maxZoom).isNotEmpty()
  }

  suspend fun estimateSizeOnDisk(bounds: Bounds): Int {
    val client = getMogClient()
    val requests = client.buildTilesRequests(bounds.toGoogleMapsObject())
    return requests.sumOf { it.totalBytes }
  }
}
