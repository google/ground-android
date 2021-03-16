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

package com.google.android.gnd.repository;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static com.google.android.gnd.util.ImmutableSetCollector.toImmutableSet;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.basemap.OfflineBaseMap;
import com.google.android.gnd.model.basemap.OfflineBaseMap.State;
import com.google.android.gnd.model.basemap.OfflineBaseMapSource;
import com.google.android.gnd.model.basemap.tile.TileSource;
import com.google.android.gnd.persistence.geojson.GeoJsonParser;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.sync.TileSourceDownloadWorkManager;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.system.GeocodingManager;
import com.google.android.gnd.ui.util.FileUtil;
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

public class OfflineBaseMapRepository {
  private final TileSourceDownloadWorkManager tileSourceDownloadWorkManager;
  private final LocalDataStore localDataStore;
  private final ProjectRepository projectRepository;
  private final GeoJsonParser geoJsonParser;
  private final FileUtil fileUtil;
  private final Schedulers schedulers;
  private final GeocodingManager geocodingManager;

  @Inject
  public OfflineBaseMapRepository(
      TileSourceDownloadWorkManager tileSourceDownloadWorkManager,
      LocalDataStore localDataStore,
      ProjectRepository projectRepository,
      GeoJsonParser geoJsonParser,
      FileUtil fileUtil,
      Schedulers schedulers,
      GeocodingManager geocodingManager) {
    this.tileSourceDownloadWorkManager = tileSourceDownloadWorkManager;
    this.localDataStore = localDataStore;
    this.geoJsonParser = geoJsonParser;
    this.projectRepository = projectRepository;
    this.fileUtil = fileUtil;
    this.schedulers = schedulers;
    this.geocodingManager = geocodingManager;
  }

  /**
   * Download the offline basemap source for the active project.
   *
   * <p>Only the first basemap source is used. Sources are always re-downloaded and overwritten on
   * subsequent calls.
   */
  private File downloadOfflineBaseMapSource(OfflineBaseMapSource offlineBaseMapSource)
      throws IOException {

    URL baseMapUrl = offlineBaseMapSource.getUrl();
    Timber.d("Basemap url: %s, file: %s", baseMapUrl, baseMapUrl.getFile());
    File localFile = fileUtil.getOrCreateFile(baseMapUrl.getFile());

    FileUtils.copyURLToFile(baseMapUrl, localFile);
    return localFile;
  }

  /** Enqueue a single area and its tile sources for download. */
  @Cold
  private Completable enqueueDownload(OfflineBaseMap area, ImmutableList<TileSource> tileSources) {
    return Flowable.fromIterable(tileSources)
        .flatMapCompletable(
            tileSource ->
                localDataStore
                    .getTileSource(tileSource.getUrl())
                    .map(TileSource::incrementAreaCount)
                    .toSingle(tileSource)
                    .flatMapCompletable(localDataStore::insertOrUpdateTileSource))
        .doOnError(__ -> Timber.e("failed to add/update a tile in the database"))
        .andThen(
            localDataStore.insertOrUpdateOfflineArea(
                area.toBuilder().setState(State.IN_PROGRESS).build()))
        .andThen(tileSourceDownloadWorkManager.enqueueTileSourceDownloadWorker());
  }

  /**
   * Determine the tile sources that need to be downloaded for a given area, then enqueue tile
   * source downloads.
   */
  @Cold
  private Completable enqueueTileSourceDownloads(OfflineBaseMap area) {
    return getBaseMapTileSources(area)
        .flatMapCompletable(tileSources -> enqueueDownload(area, tileSources))
        .doOnComplete(() -> Timber.d("area download completed"))
        .doOnError(throwable -> Timber.e(throwable, "failed to download area"))
        .subscribeOn(schedulers.io());
  }

  /**
   * Get a list of tile sources specified in the first basemap source of the active project that
   * intersect a given area.
   */
  @Cold
  private Single<ImmutableList<TileSource>> getBaseMapTileSources(OfflineBaseMap offlineBaseMap) {
    LatLngBounds bounds = offlineBaseMap.getBounds();

    // TODO: Simplify this stream.
    return projectRepository
        .getProjectLoadingState()
        .compose(Loadable::values)
        .map(Project::getOfflineBaseMapSources)
        .doOnError(
            throwable -> Timber.e(throwable, "no basemap sources specified for the active project"))
        .map(ImmutableList::asList)
        .flatMap(Flowable::fromIterable)
        .firstOrError()
        .map(this::downloadOfflineBaseMapSource)
        .map(json -> geoJsonParser.intersectingTiles(bounds, json))
        .doOnError(
            throwable ->
                Timber.e(throwable, "couldn't retrieve basemap sources for the active project"));
  }

  @Cold
  public Completable addAreaAndEnqueue(OfflineBaseMap baseMap) {
    return geocodingManager
        .getAreaName(baseMap.getBounds())
        .map(name -> baseMap.toBuilder().setName(name).build())
        .flatMapCompletable(this::enqueueTileSourceDownloads);
  }

  /**
   * Retrieves all offline areas from the local store and continually streams the list as the
   * local store is updated.
   * Triggers `onError` only if there is a problem accessing the local store.
   * */
  @Cold(terminates = false)
  public Flowable<ImmutableList<OfflineBaseMap>> getOfflineAreasOnceAndStream() {
    return localDataStore.getOfflineAreasOnceAndStream();
  }

  /**
   * Fetches a single offline area by ID.
   * Triggers `onError` when the area is not found.
   * Triggers `onSuccess` when the area is found.
   * */
  @Cold
  public Single<OfflineBaseMap> getOfflineArea(String offlineAreaId) {
    return localDataStore.getOfflineAreaById(offlineAreaId);
  }

  /**
   * Returns the intersection of downloaded tiles in two collections of tiles. Tiles are considered
   * equal if their URLs are equal.
   */
  private ImmutableSet<TileSource> downloadedTileSourcesIntersection(
      Collection<TileSource> tileSources, Collection<TileSource> other) {
    ImmutableList<String> otherUrls =
        stream(other).map(TileSource::getUrl).collect(toImmutableList());

    return stream(tileSources)
        .filter(tile -> otherUrls.contains(tile.getUrl()))
        .filter(tile -> tile.getState() == TileSource.State.DOWNLOADED)
        .collect(toImmutableSet());
  }

  /**
   * Retrieves a the set of downloaded tiles that intersect with {@param offlineBaseMap} and
   * continually streams the set as the local store is updated.
   * Triggers `onError` only if there is a problem accessing the local store.
   * */
  @Cold(terminates = false)
  public Flowable<ImmutableSet<TileSource>> getIntersectingDownloadedTileSourcesOnceAndStream(
      OfflineBaseMap offlineBaseMap) {
    return getBaseMapTileSources(offlineBaseMap)
        .flatMapPublisher(
            tiles ->
                getDownloadedTileSourcesOnceAndStream()
                    .map(ts -> downloadedTileSourcesIntersection(ts, tiles)))
        // If no tile sources are found, we report the area takes up 0.0mb on the device.
        .doOnError(
            throwable -> Timber.d(throwable, "no tile sources found for area %s", offlineBaseMap))
        .onErrorReturn(__ -> ImmutableSet.of());
  }

  /**
   * Retrieves a set of downloaded tiles that intersect with {@param offlineBaseMap}.
   * Triggers `onError` only if there is a problem accessing the local store.
   * */
  @Cold
  public Maybe<ImmutableSet<TileSource>> getIntersectingDownloadedTileSourcesOnce(
      OfflineBaseMap offlineBaseMap) {
    return getIntersectingDownloadedTileSourcesOnceAndStream(offlineBaseMap).firstElement();
  }

  /**
   * Retrieves all downloaded tile sources from the local store.
   * Triggers `onError` only if there is a problem accessing the local store;
   * does not trigger an error on empty rows.
   * */
  @Cold(terminates = false)
  public Flowable<ImmutableSet<TileSource>> getDownloadedTileSourcesOnceAndStream() {
    return localDataStore
        .getTileSourcesOnceAndStream()
        .map(
            set ->
                stream(set)
                    .filter(tileSource -> tileSource.getState() == TileSource.State.DOWNLOADED)
                    .collect(toImmutableSet()));
  }

  /**
   * Delete an offline base map and any tile sources associated with it that do not overlap with
   * other offline base maps .
   */
  @Cold
  public Completable deleteArea(String offlineAreaId) {
    return localDataStore
        .getOfflineAreaById(offlineAreaId)
        .flatMapMaybe(this::getIntersectingDownloadedTileSourcesOnce)
        .flatMapObservable(Observable::fromIterable)
        .map(TileSource::decrementAreaCount)
        .flatMapCompletable(
            tile ->
                localDataStore
                    .updateTileSourceBasemapReferenceCountByUrl(
                        tile.getBasemapReferenceCount(), tile.getUrl())
                    .andThen(localDataStore.deleteTileByUrl(tile)))
        .andThen(localDataStore.deleteOfflineArea(offlineAreaId));
  }
}
