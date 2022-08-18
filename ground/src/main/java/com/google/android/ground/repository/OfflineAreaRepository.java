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

package com.google.android.ground.repository;

import static com.google.android.ground.util.ImmutableListCollector.toImmutableList;
import static com.google.android.ground.util.ImmutableSetCollector.toImmutableSet;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.basemap.BaseMap;
import com.google.android.ground.model.basemap.OfflineArea;
import com.google.android.ground.model.basemap.OfflineArea.State;
import com.google.android.ground.model.basemap.tile.TileSet;
import com.google.android.ground.persistence.local.LocalDataStore;
import com.google.android.ground.persistence.mbtiles.MbtilesFootprintParser;
import com.google.android.ground.persistence.sync.TileSetDownloadWorkManager;
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator;
import com.google.android.ground.rx.Loadable;
import com.google.android.ground.rx.Schedulers;
import com.google.android.ground.rx.annotations.Cold;
import com.google.android.ground.system.GeocodingManager;
import com.google.android.ground.ui.util.FileUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import timber.log.Timber;

public class OfflineAreaRepository {
  private final TileSetDownloadWorkManager tileSetDownloadWorkManager;
  private final LocalDataStore localDataStore;
  private final SurveyRepository surveyRepository;
  private final MbtilesFootprintParser geoJsonParser;
  private final FileUtil fileUtil;
  private final Schedulers schedulers;
  private final GeocodingManager geocodingManager;

  private final OfflineUuidGenerator offlineUuidGenerator;

  @Inject
  public OfflineAreaRepository(
      TileSetDownloadWorkManager tileSetDownloadWorkManager,
      LocalDataStore localDataStore,
      SurveyRepository surveyRepository,
      MbtilesFootprintParser geoJsonParser,
      FileUtil fileUtil,
      Schedulers schedulers,
      GeocodingManager geocodingManager,
      OfflineUuidGenerator offlineUuidGenerator) {
    this.tileSetDownloadWorkManager = tileSetDownloadWorkManager;
    this.localDataStore = localDataStore;
    this.geoJsonParser = geoJsonParser;
    this.surveyRepository = surveyRepository;
    this.fileUtil = fileUtil;
    this.schedulers = schedulers;
    this.geocodingManager = geocodingManager;
    this.offlineUuidGenerator = offlineUuidGenerator;
  }

  /**
   * Download the offline basemap source for the active survey.
   *
   * <p>Only the first basemap source is used. Sources are always re-downloaded and overwritten on
   * subsequent calls.
   */
  private File downloadOfflineBaseMapSource(BaseMap baseMap) throws IOException {

    URL baseMapUrl = baseMap.getUrl();
    Timber.d("Basemap url: %s, file: %s", baseMapUrl, baseMapUrl.getFile());
    File localFile = fileUtil.getOrCreateFile(baseMapUrl.getFile());

    FileUtils.copyURLToFile(baseMapUrl, localFile);
    return localFile;
  }

  /** Enqueue a single area and its tile sources for download. */
  @Cold
  private Completable enqueueDownload(OfflineArea area, ImmutableList<TileSet> tileSets) {
    return Flowable.fromIterable(tileSets)
        .flatMapCompletable(
            tileSet ->
                localDataStore
                    .getTileSet(tileSet.getUrl())
                    .map(TileSet::incrementOfflineAreaCount)
                    .toSingle(tileSet)
                    .flatMapCompletable(localDataStore::insertOrUpdateTileSet))
        .doOnError(__ -> Timber.e("failed to add/update a tile in the database"))
        .andThen(
            localDataStore.insertOrUpdateOfflineArea(
                // TODO: When this class is converted to kotlin we can simply pass a named state
                //  parameter to area.copy()
                new OfflineArea(area.getId(), State.IN_PROGRESS, area.getBounds(), area.getName())))
        .andThen(tileSetDownloadWorkManager.enqueueTileSetDownloadWorker());
  }

  /**
   * Determine the tile sources that need to be downloaded for a given area, then enqueue tile
   * source downloads.
   */
  @Cold
  private Completable enqueueTileSetDownloads(OfflineArea area) {
    return getOfflineAreaTileSets(area)
        .flatMapCompletable(tileSets -> enqueueDownload(area, tileSets))
        .doOnComplete(() -> Timber.d("area download completed"))
        .doOnError(throwable -> Timber.e(throwable, "failed to download area"))
        .subscribeOn(schedulers.io());
  }

  /**
   * Get a list of tile sources specified in the first basemap source of the active survey that
   * intersect a given area.
   */
  @Cold
  private Single<ImmutableList<TileSet>> getOfflineAreaTileSets(OfflineArea offlineArea) {
    LatLngBounds bounds = offlineArea.getBounds();

    // TODO: Simplify this stream.
    return surveyRepository
        .getSurveyLoadingState()
        .compose(Loadable::values)
        .map(Survey::getBaseMaps)
        .doOnError(
            throwable -> Timber.e(throwable, "no basemap sources specified for the active survey"))
        .map(ImmutableList::asList)
        .flatMap(Flowable::fromIterable)
        .firstOrError()
        .map(this::downloadOfflineBaseMapSource)
        .flatMap(json -> geoJsonParser.intersectingTiles(bounds, json))
        .doOnError(
            throwable ->
                Timber.e(throwable, "couldn't retrieve basemap sources for the active survey"));
  }

  @Cold
  public Completable addOfflineAreaAndEnqueue(OfflineArea area) {
    return geocodingManager
        .getAreaName(area.getBounds())
        // TODO: When this class is converted to kotlin we can simply pass a named "name"
        //  parameter to area.copy()
        .map(name -> new OfflineArea(area.getId(), State.IN_PROGRESS, area.getBounds(), name))
        .flatMapCompletable(this::enqueueTileSetDownloads);
  }

  /**
   * Retrieves all offline areas from the local store and continually streams the list as the local
   * store is updated. Triggers `onError` only if there is a problem accessing the local store.
   */
  @Cold(terminates = false)
  public Flowable<ImmutableList<OfflineArea>> getOfflineAreasOnceAndStream() {
    return localDataStore.getOfflineAreasOnceAndStream();
  }

  /**
   * Fetches a single offline area by ID. Triggers `onError` when the area is not found. Triggers
   * `onSuccess` when the area is found.
   */
  @Cold
  public Single<OfflineArea> getOfflineArea(String offlineAreaId) {
    return localDataStore.getOfflineAreaById(offlineAreaId);
  }

  /**
   * Returns the intersection of downloaded tiles in two collections of tiles. Tiles are considered
   * equal if their URLs are equal.
   */
  private ImmutableSet<TileSet> downloadedTileSetsIntersection(
      Collection<TileSet> tileSets, Collection<TileSet> other) {
    ImmutableList<String> otherUrls = stream(other).map(TileSet::getUrl).collect(toImmutableList());

    return stream(tileSets)
        .filter(tile -> otherUrls.contains(tile.getUrl()))
        .filter(tile -> tile.getState() == TileSet.State.DOWNLOADED)
        .collect(toImmutableSet());
  }

  /**
   * Retrieves a the set of downloaded tiles that intersect with {@param offlineArea} and
   * continually streams the set as the local store is updated. Triggers `onError` only if there is
   * a problem accessing the local store.
   */
  @Cold(terminates = false)
  public Flowable<ImmutableSet<TileSet>> getIntersectingDownloadedTileSetsOnceAndStream(
      OfflineArea offlineArea) {
    return getOfflineAreaTileSets(offlineArea)
        .flatMapPublisher(
            tiles ->
                getDownloadedTileSetsOnceAndStream()
                    .map(ts -> downloadedTileSetsIntersection(ts, tiles)))
        // If no tile sources are found, we report the area takes up 0.0mb on the device.
        .doOnError(
            throwable -> Timber.d(throwable, "no tile sources found for area %s", offlineArea))
        .onErrorReturn(__ -> ImmutableSet.of());
  }

  /**
   * Retrieves a set of downloaded tiles that intersect with {@param offlineArea}. Triggers
   * `onError` only if there is a problem accessing the local store.
   */
  @Cold
  public Maybe<ImmutableSet<TileSet>> getIntersectingDownloadedTileSetsOnce(
      OfflineArea offlineArea) {
    return getIntersectingDownloadedTileSetsOnceAndStream(offlineArea).firstElement();
  }

  /**
   * Retrieves all downloaded tile sources from the local store. Triggers `onError` only if there is
   * a problem accessing the local store; does not trigger an error on empty rows.
   */
  @Cold(terminates = false)
  public Flowable<ImmutableSet<TileSet>> getDownloadedTileSetsOnceAndStream() {
    return localDataStore
        .getTileSetsOnceAndStream()
        .map(
            set ->
                stream(set)
                    .filter(tileSet -> tileSet.getState() == TileSet.State.DOWNLOADED)
                    .collect(toImmutableSet()));
  }

  /**
   * Delete an offline area and any tile sources associated with it that do not overlap with other
   * offline base maps .
   */
  @Cold
  public Completable deleteOfflineArea(String offlineAreaId) {
    return localDataStore
        .getOfflineAreaById(offlineAreaId)
        .flatMapMaybe(this::getIntersectingDownloadedTileSetsOnce)
        .flatMapObservable(Observable::fromIterable)
        .map(TileSet::decrementOfflineAreaCount)
        .flatMapCompletable(
            tile ->
                localDataStore
                    .updateTileSetOfflineAreaReferenceCountByUrl(
                        tile.getOfflineAreaReferenceCount(), tile.getUrl())
                    .andThen(localDataStore.deleteTileSetByUrl(tile)))
        .andThen(localDataStore.deleteOfflineArea(offlineAreaId));
  }

  /**
   * Retrieves all tile sources from a GeoJSON basemap specification, regardless of their
   * coordinates.
   */
  public Single<ImmutableList<TileSet>> getTileSets() {
    return surveyRepository
        .getSurveyLoadingState()
        .compose(Loadable::values)
        .map(Survey::getBaseMaps)
        .doOnError(t -> Timber.e(t, "No basemap sources specified for the active survey"))
        .map(ImmutableList::asList)
        .flatMap(Flowable::fromIterable)
        .firstOrError()
        .flatMap(this::getTileSets)
        .doOnError(t -> Timber.e(t, "Couldn't retrieve basemap sources for the active survey"));
  }

  /**
   * Returns a list of {@link TileSet}s corresponding to a given {@link BaseMap} based on the
   * BaseMap's type.
   *
   * <p>This function may perform network IO when the provided BaseMap requires downloading TileSets
   * locally.
   */
  private Single<ImmutableList<TileSet>> getTileSets(BaseMap baseMap) throws IOException {
    switch (baseMap.getType()) {
      case MBTILES_FOOTPRINTS:
        File tileFile = downloadOfflineBaseMapSource(baseMap);
        return geoJsonParser.allTiles(tileFile);
      case TILED_WEB_MAP:
        return Single.just(
            ImmutableList.of(
                new TileSet(
                    baseMap.getUrl().toString(),
                    offlineUuidGenerator.generateUuid(),
                    baseMap.getUrl().toString(),
                    TileSet.State.PENDING,
                    1)));
      default:
        Timber.d("Unknown basemap source type");
        // Try to read a tile from the URL anyway.
        return Single.just(
            ImmutableList.of(
                new TileSet(
                    baseMap.getUrl().toString(),
                    offlineUuidGenerator.generateUuid(),
                    baseMap.getUrl().toString(),
                    TileSet.State.PENDING,
                    1)));
    }
  }
}
