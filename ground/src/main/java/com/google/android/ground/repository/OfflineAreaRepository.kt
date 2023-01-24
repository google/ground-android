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
import com.google.android.ground.model.basemap.BaseMap
import com.google.android.ground.model.basemap.BaseMap.BaseMapType
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.mbtiles.MbtilesFootprintParser
import com.google.android.ground.persistence.sync.TileSetDownloadWorkManager
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.system.GeocodingManager
import com.google.android.ground.ui.util.FileUtil
import com.google.android.ground.util.toImmutableList
import com.google.android.ground.util.toImmutableSet
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.*
import java.io.File
import java.io.IOException
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import timber.log.Timber

class OfflineAreaRepository
@Inject
constructor(
  private val tileSetDownloadWorkManager: TileSetDownloadWorkManager,
  private val localDataStore: LocalDataStore,
  private val surveyRepository: SurveyRepository,
  private val geoJsonParser: MbtilesFootprintParser,
  private val fileUtil: FileUtil,
  private val schedulers: Schedulers,
  private val geocodingManager: GeocodingManager,
  private val offlineUuidGenerator: OfflineUuidGenerator
) {
  private val offlineAreaStore = localDataStore.localOfflineAreaStore
  private val tileSetStore = localDataStore.tileSetStore

  /**
   * Download the offline basemap source for the active survey.
   *
   * Only the first basemap source is used. Sources are always re-downloaded and overwritten on
   * subsequent calls.
   */
  @Throws(IOException::class)
  private fun downloadOfflineBaseMapSource(baseMap: BaseMap): File {
    val baseMapUrl = baseMap.url
    Timber.d("Basemap url: $baseMapUrl, file: ${baseMapUrl.file}")
    val localFile = fileUtil.getOrCreateFile(baseMapUrl.file)

    FileUtils.copyURLToFile(baseMapUrl, localFile)
    return localFile
  }

  /** Enqueue a single area and its tile sources for download. */
  private fun enqueueDownload(
    area: OfflineArea,
    tileSets: ImmutableList<TileSet>
  ): @Cold Completable =
    Flowable.fromIterable(tileSets)
      .flatMapCompletable { tileSet ->
        tileSetStore
          .getTileSet(tileSet.url)
          .map { it.incrementOfflineAreaCount() }
          .toSingle(tileSet)
          .flatMapCompletable { tileSetStore.insertOrUpdateTileSet(it) }
      }
      .doOnError { Timber.e("failed to add/update a tile in the database") }
      .andThen(
        offlineAreaStore.insertOrUpdateOfflineArea(area.copy(state = OfflineArea.State.IN_PROGRESS))
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
  private fun getOfflineAreaTileSets(
    offlineArea: OfflineArea
  ): @Cold Single<ImmutableList<TileSet>> =
    surveyRepository.activeSurvey
      .map { it.map(Survey::baseMaps).orElse(ImmutableList.of()) }
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
  val offlineAreasOnceAndStream: @Cold(terminates = false) Flowable<ImmutableList<OfflineArea>>
    get() = offlineAreaStore.offlineAreasOnceAndStream

  /**
   * Fetches a single offline area by ID. Triggers `onError` when the area is not found. Triggers
   * `onSuccess` when the area is found.
   */
  fun getOfflineArea(offlineAreaId: String): @Cold Single<OfflineArea> =
    offlineAreaStore.getOfflineAreaById(offlineAreaId)

  /**
   * Returns the intersection of downloaded tiles in two collections of tiles. Tiles are considered
   * equal if their URLs are equal.
   */
  private fun downloadedTileSetsIntersection(
    tileSets: Collection<TileSet>,
    other: Collection<TileSet>
  ): ImmutableSet<TileSet> {
    val otherUrls = other.map { it.url }.toImmutableList()

    return tileSets
      .filter { otherUrls.contains(it.url) }
      .filter { it.state === TileSet.State.DOWNLOADED }
      .toImmutableSet()
  }

  /**
   * Retrieves a the set of downloaded tiles that intersect with {@param offlineArea} and
   * continually streams the set as the local store is updated. Triggers `onError` only if there is
   * a problem accessing the local store.
   */
  fun getIntersectingDownloadedTileSetsOnceAndStream(
    offlineArea: OfflineArea
  ): @Cold(terminates = false) Flowable<ImmutableSet<TileSet>> =
    getOfflineAreaTileSets(offlineArea)
      .flatMapPublisher { tiles ->
        downloadedTileSetsOnceAndStream.map { tileSet ->
          downloadedTileSetsIntersection(tileSet, tiles)
        }
      } // If no tile sources are found, we report the area takes up 0.0mb on the device.
      .doOnError { throwable -> Timber.d(throwable, "no tile sources found for area $offlineArea") }
      .onErrorReturn { ImmutableSet.of() }

  /**
   * Retrieves a set of downloaded tiles that intersect with {@param offlineArea}. Triggers
   * `onError` only if there is a problem accessing the local store.
   */
  fun getIntersectingDownloadedTileSetsOnce(
    offlineArea: OfflineArea
  ): @Cold Maybe<ImmutableSet<TileSet>> =
    getIntersectingDownloadedTileSetsOnceAndStream(offlineArea).firstElement()

  /**
   * Retrieves all downloaded tile sources from the local store. Triggers `onError` only if there is
   * a problem accessing the local store; does not trigger an error on empty rows.
   */
  val downloadedTileSetsOnceAndStream: @Cold(terminates = false) Flowable<ImmutableSet<TileSet>>
    get() =
      tileSetStore.tileSetsOnceAndStream.map { tileSet ->
        tileSet.filter { it.state === TileSet.State.DOWNLOADED }.toImmutableSet()
      }

  /**
   * Delete an offline area and any tile sources associated with it that do not overlap with other
   * offline base maps .
   */
  fun deleteOfflineArea(offlineAreaId: String): @Cold Completable =
    offlineAreaStore
      .getOfflineAreaById(offlineAreaId)
      .flatMapMaybe { offlineArea -> getIntersectingDownloadedTileSetsOnce(offlineArea) }
      .flatMapObservable { source -> Observable.fromIterable(source) }
      .map { it.decrementOfflineAreaCount() }
      .flatMapCompletable { tileSet ->
        tileSetStore
          .updateTileSetOfflineAreaReferenceCountByUrl(
            tileSet.offlineAreaReferenceCount,
            tileSet.url
          )
          .andThen(tileSetStore.deleteTileSetByUrl(tileSet))
      }
      .andThen(offlineAreaStore.deleteOfflineArea(offlineAreaId))

  /**
   * Retrieves all tile sources from a GeoJSON basemap specification, regardless of their
   * coordinates.
   */
  val tileSets: Single<ImmutableList<TileSet>>
    get() =
      surveyRepository.activeSurvey
        .map { it.map(Survey::baseMaps).orElse(ImmutableList.of()) }
        .doOnError { t -> Timber.e(t, "No basemap sources specified for the active survey") }
        .flatMap { source -> Flowable.fromIterable(source) }
        .firstOrError()
        .flatMap { baseMap -> getTileSets(baseMap) }
        .doOnError { t -> Timber.e(t, "Couldn't retrieve basemap sources for the active survey") }

  /**
   * Returns a list of [TileSet]s corresponding to a given [BaseMap] based on the BaseMap's type.
   *
   * This function may perform network IO when the provided BaseMap requires downloading TileSets
   * locally.
   */
  @Throws(IOException::class)
  private fun getTileSets(baseMap: BaseMap): Single<ImmutableList<TileSet>> =
    when (baseMap.type) {
      BaseMapType.MBTILES_FOOTPRINTS -> {
        val tileFile = downloadOfflineBaseMapSource(baseMap)
        geoJsonParser.allTiles(tileFile)
      }
      BaseMapType.TILED_WEB_MAP ->
        Single.just(
          ImmutableList.of(
            TileSet(
              baseMap.url.toString(),
              offlineUuidGenerator.generateUuid(),
              baseMap.url.toString(),
              TileSet.State.PENDING,
              1
            )
          )
        )
      else -> {
        Timber.d("Unknown basemap source type")
        // Try to read a tile from the URL anyway.
        Single.just(
          ImmutableList.of(
            TileSet(
              baseMap.url.toString(),
              offlineUuidGenerator.generateUuid(),
              baseMap.url.toString(),
              TileSet.State.PENDING,
              1
            )
          )
        )
      }
    }
}
