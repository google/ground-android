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

import com.google.android.ground.model.Survey
import com.google.android.ground.model.imagery.MbtilesFile
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.persistence.local.stores.LocalOfflineAreaStore
import com.google.android.ground.persistence.local.stores.LocalTileSetStore
import com.google.android.ground.persistence.mbtiles.MbtilesFootprintParser
import com.google.android.ground.persistence.sync.TileSetDownloadWorkManager
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.GeocodingManager
import com.google.android.ground.ui.map.gms.mog.*
import com.google.android.ground.ui.util.FileUtil
import io.reactivex.*
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import org.apache.commons.io.FileUtils
import timber.log.Timber

@Singleton
class OfflineAreaRepository
@Inject
constructor(
  private val tileSetDownloadWorkManager: TileSetDownloadWorkManager,
  private val localOfflineAreaStore: LocalOfflineAreaStore,
  private val localTileSetStore: LocalTileSetStore,
  private val surveyRepository: SurveyRepository,
  private val geoJsonParser: MbtilesFootprintParser,
  private val fileUtil: FileUtil,
  private val schedulers: Schedulers,
  private val geocodingManager: GeocodingManager
) {

  /**
   * Download the offline basemap source for the active survey.
   *
   * Only the first basemap source is used. Sources are always re-downloaded and overwritten on
   * subsequent calls.
   */
  @Throws(IOException::class)
  private fun downloadOfflineBaseMapSource(tileSource: TileSource): File {
    val baseMapUrl = tileSource.url
    Timber.d("Basemap url: $baseMapUrl, file: ${baseMapUrl.file}")
    val localFile = fileUtil.getOrCreateFile(baseMapUrl.file)

    FileUtils.copyURLToFile(baseMapUrl, localFile)
    return localFile
  }

  /** Enqueue a single area and its tile sources for download. */
  private fun enqueueDownload(
    area: OfflineArea,
    mbtilesFiles: List<MbtilesFile>
  ): @Cold Completable =
    Flowable.fromIterable(mbtilesFiles)
      .flatMapCompletable { tileSet ->
        localTileSetStore
          .getTileSet(tileSet.url)
          .map { it.incrementReferenceCount() }
          .toSingle(tileSet)
          .flatMapCompletable { localTileSetStore.insertOrUpdateTileSet(it) }
      }
      .doOnError { Timber.e("failed to add/update a tile in the database") }
      .andThen(
        localOfflineAreaStore.insertOrUpdateOfflineArea(
          area.copy(state = OfflineArea.State.IN_PROGRESS)
        )
      )
      .andThen(tileSetDownloadWorkManager.enqueueTileSetDownloadWorker())

  /**
   * Determine the tile sources that need to be downloaded for a given area, then enqueue tile
   * source downloads.
   */
  private fun enqueueTileSetDownloads(area: OfflineArea): @Cold Completable =
    getOfflineAreaTileSets(area)
      .flatMapCompletable { tileSets -> enqueueDownload(area, tileSets) }
      .doOnComplete { Timber.d("area download completed") }
      .doOnError { throwable -> Timber.e(throwable, "failed to download area") }
      .subscribeOn(schedulers.io())

  /**
   * Get a list of tile sources specified in the first basemap source of the active survey that
   * intersect a given area.
   */
  // TODO: Simplify this stream.
  private fun getOfflineAreaTileSets(offlineArea: OfflineArea): @Cold Single<List<MbtilesFile>> =
    surveyRepository.activeSurveyFlowable
      .map { it.map(Survey::tileSources).orElse(listOf()) }
      .doOnError { throwable ->
        Timber.e(throwable, "no basemap sources specified for the active survey")
      }
      .flatMap { source -> Flowable.fromIterable(source) }
      .firstOrError()
      .map { baseMap -> downloadOfflineBaseMapSource(baseMap) }
      .flatMap { json -> geoJsonParser.intersectingTiles(offlineArea.bounds, json) }
      .doOnError { throwable ->
        Timber.e(throwable, "couldn't retrieve basemap sources for the active survey")
      }

  fun addOfflineAreaAndEnqueue(area: OfflineArea): @Cold Completable =
    geocodingManager
      .getAreaName(area.bounds)
      .map { name -> area.copy(state = OfflineArea.State.IN_PROGRESS, name = name) }
      .flatMapCompletable { enqueueTileSetDownloads(it) }

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
   * Returns the intersection of downloaded tiles in two collections of tiles. Tiles are considered
   * equal if their URLs are equal.
   */
  private fun downloadedTileSetsIntersection(
    mbtilesFiles: Collection<MbtilesFile>,
    other: Collection<MbtilesFile>
  ): Set<MbtilesFile> {
    val otherUrls = other.map { it.url }
    return mbtilesFiles
      .filter { otherUrls.contains(it.url) }
      .filter { it.downloadState === MbtilesFile.DownloadState.DOWNLOADED }
      .toPersistentSet()
  }

  /**
   * Retrieves a the set of downloaded tiles that intersect with {@param offlineArea} and
   * continually streams the set as the local store is updated. Triggers `onError` only if there is
   * a problem accessing the local store.
   */
  fun getIntersectingDownloadedTileSetsOnceAndStream(
    offlineArea: OfflineArea
  ): @Cold(terminates = false) Flowable<Set<MbtilesFile>> =
    getOfflineAreaTileSets(offlineArea)
      .flatMapPublisher { tiles ->
        downloadedTileSetsOnceAndStream().map { tileSet ->
          downloadedTileSetsIntersection(tileSet, tiles)
        }
      } // If no tile sources are found, we report the area takes up 0.0mb on the device.
      .doOnError { throwable -> Timber.d(throwable, "no tile sources found for area $offlineArea") }
      .onErrorReturn { setOf() }

  /**
   * Retrieves a set of downloaded tiles that intersect with {@param offlineArea}. Triggers
   * `onError` only if there is a problem accessing the local store.
   */
  fun getIntersectingDownloadedTileSetsOnce(
    offlineArea: OfflineArea
  ): @Cold Maybe<Set<MbtilesFile>> =
    getIntersectingDownloadedTileSetsOnceAndStream(offlineArea).firstElement()

  /**
   * Retrieves all downloaded tile sources from the local store. Triggers `onError` only if there is
   * a problem accessing the local store; does not trigger an error on empty rows.
   */
  fun downloadedTileSetsOnceAndStream(): @Cold(terminates = false) Flowable<Set<MbtilesFile>> =
    localTileSetStore.tileSetsOnceAndStream().map { tileSet ->
      tileSet.filter { it.downloadState === MbtilesFile.DownloadState.DOWNLOADED }.toPersistentSet()
    }

  /**
   * Delete an offline area and any tile sources associated with it that do not overlap with other
   * offline base maps .
   */
  fun deleteOfflineArea(offlineAreaId: String): @Cold Completable =
    localOfflineAreaStore
      .getOfflineAreaById(offlineAreaId)
      .flatMapMaybe { offlineArea -> getIntersectingDownloadedTileSetsOnce(offlineArea) }
      .flatMapObservable { source -> Observable.fromIterable(source) }
      .map { it.decrementReferenceCount() }
      .flatMapCompletable { tileSet ->
        localTileSetStore
          .updateTileSetOfflineAreaReferenceCountByUrl(tileSet.referenceCount, tileSet.url)
          .andThen(localTileSetStore.deleteTileSetByUrl(tileSet))
      }
      .andThen(localOfflineAreaStore.deleteOfflineArea(offlineAreaId))

  /**
   * Uses the first tile source URL of the currently active survey and returns a [MogClient], or
   * throws an error if no survey is active or if no tile sources are defined.
   */
  suspend fun getMogClient(): MogClient {
    // TODO(#1730): Make sub-paths configurable and stop hardcoding here.
    val baseUrl = getFirstTileSourceUrl()
    val mogCollection =
      MogCollection(
        listOf(MogSource("${baseUrl}/world.tif", 0..7), MogSource("${baseUrl}/{x}/{y}.tif", 8..14))
      )
    return MogClient(mogCollection)
  }

  /**
   * Returns the URL of the first tile source in the current survey, or throws an error if no survey
   * is active or if no tile sources are defined.
   */
  private suspend fun getFirstTileSourceUrl(): String {
    val survey = surveyRepository.activeSurveyFlowable.asFlow().firstOrNull()?.orElse(null)
    return survey?.tileSources?.firstOrNull()?.url?.toString()
      ?: error("Survey has no tile sources")
  }

  /**
   * Sends the provided tile requests and downloads tiles to the local file store. The path will be
   * interpreted as a sub-path relative to the app's file path.
   */
  suspend fun downloadTiles(client: MogClient, requests: List<MogTilesRequest>): Flow<Int> {
    // TODO(#1730): Generate local tiles path based on source base path.
    val tilePath = File(fileUtil.filesDir.path, "tiles").path
    val downloader = MogTileDownloader(client, tilePath)
    return downloader.downloadTiles(requests)
  }
}
