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
import com.google.android.gnd.Config;
import com.google.android.gnd.R;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.model.basemap.OfflineArea.State;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.persistence.geojson.GeoJsonParser;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.sync.TileDownloadWorkManager;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.ui.util.FileUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import timber.log.Timber;

public class OfflineAreaRepository {
  private final TileDownloadWorkManager tileDownloadWorkManager;
  private final LocalDataStore localDataStore;
  private final GeoJsonParser geoJsonParser;
  private final FileUtil fileUtil;

  private final OfflineUuidGenerator uuidGenerator;

  @Inject
  public OfflineAreaRepository(
      TileDownloadWorkManager tileDownloadWorkManager,
      LocalDataStore localDataStore,
      GeoJsonParser geoJsonParser,
      OfflineUuidGenerator uuidGenerator,
      FileUtil fileUtil) {
    this.tileDownloadWorkManager = tileDownloadWorkManager;
    this.localDataStore = localDataStore;
    this.geoJsonParser = geoJsonParser;
    this.uuidGenerator = uuidGenerator;
    this.fileUtil = fileUtil;
  }

  private Completable enqueueTileDownloads(OfflineArea area) {
    File jsonSource;

    try {
      jsonSource = fileUtil.getFileFromRawResource(R.raw.gnd_geojson, Config.GEO_JSON);
    } catch (IOException e) {
      return Completable.error(e);
    }

    ImmutableList<Tile> tiles = geoJsonParser.intersectingTiles(area.getBounds(), jsonSource);

    return localDataStore
        .insertOrUpdateOfflineArea(area.toBuilder().setState(State.IN_PROGRESS).build())
        .andThen(
            Completable.merge(
                stream(tiles.asList())
                    .map(localDataStore::insertOrUpdateTile)
                    .collect(toImmutableList())))
        .doOnError(__ -> Timber.e("failed to add/update a tile in the database"))
        .andThen(tileDownloadWorkManager.enqueueTileDownloadWorker());
  }

  public Completable addAreaAndEnqueue(LatLngBounds bounds) {
    OfflineArea offlineArea =
        OfflineArea.newBuilder()
            .setBounds(bounds)
            .setId(uuidGenerator.generateUuid())
            .setState(State.PENDING)
            .build();

    return localDataStore
        .insertOrUpdateOfflineArea(offlineArea)
        .doOnError(__ -> Timber.e("failed to add/update offline area in the database"))
        .andThen(enqueueTileDownloads(offlineArea));
  }

  public Flowable<ImmutableList<OfflineArea>> getOfflineAreasOnceAndStream() {
    return localDataStore.getOfflineAreasOnceAndStream();
  }

  public Single<OfflineArea> getOfflineArea(String offlineAreaId) {
    return localDataStore.getOfflineAreaById(offlineAreaId);
  }

  public Flowable<ImmutableSet<Tile>> getIntersectingDownloadedTilesOnceAndStream(
      OfflineArea offlineArea) {
    File jsonSource;

    try {
      jsonSource = fileUtil.getFileFromRawResource(R.raw.gnd_geojson, Config.GEO_JSON);
    } catch (IOException e) {
      return Flowable.error(e);
    }

    ImmutableList<Tile> tiles =
        geoJsonParser.intersectingTiles(offlineArea.getBounds(), jsonSource);

    return getDownloadedTilesOnceAndStream()
        .map(ts -> stream(tiles).filter(tiles::contains).collect(toImmutableSet()));
  }

  public Flowable<ImmutableSet<Tile>> getDownloadedTilesOnceAndStream() {
    return localDataStore
        .getTilesOnceAndStream()
        .map(
            set ->
                stream(set)
                    .filter(tile -> tile.getState() == Tile.State.DOWNLOADED)
                    .collect(toImmutableSet()));
  }
}
