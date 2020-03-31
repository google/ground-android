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
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.Config;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.model.basemap.OfflineArea.State;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.persistence.geojson.GeoJsonParser;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.ui.util.FileUtil;
import com.google.android.gnd.workers.TileDownloadWorkManager;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import java.io.File;
import java.io.FileNotFoundException;
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
      jsonSource = fileUtil.getFile(Config.GEO_JSON);
    } catch (FileNotFoundException e) {
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
}
