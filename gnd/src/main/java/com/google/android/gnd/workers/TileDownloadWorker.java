/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.model.basemap.tile.Tile.State;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import timber.log.Timber;

/**
 * A worker that downloads files to the device in the background. The target URL and file name are
 * provided in a {@link Data} object. This worker should only run when the device has a network
 * connection.
 */
public class TileDownloadWorker extends Worker {
  private static final String TAG = TileDownloadWorker.class.getSimpleName();

  private static final int BUFFER_SIZE = 4096;

  private final Context context;
  private final LocalDataStore localDataStore;

  class TileDownloadException extends RuntimeException {

    TileDownloadException(String msg, Throwable e) {
      super(msg, e);
    }
  }

  public TileDownloadWorker(
      @NonNull Context context, @NonNull WorkerParameters params, LocalDataStore localDataStore) {
    super(context, params);
    this.context = context;
    this.localDataStore = localDataStore;
  }

  /**
   * Given a tile, downloads the given {@param tile}'s source file and saves it to the device's app
   * storage. Optional HTTP request header {@param requestProperties} may be provided.
   */
  private void downloadTileFile(Tile tile, Map<String, String> requestProperties)
    throws TileDownloadException {
    try {
      URL url = new URL(tile.getUrl());
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      if (!requestProperties.isEmpty()) {
        for (Map.Entry<String, String> property : requestProperties.entrySet()) {
          connection.setRequestProperty(property.getKey(), property.getValue());
        }
      }

      connection.connect();

      try (InputStream is = connection.getInputStream();
        FileOutputStream fos = context.openFileOutput(tile.getPath(), Context.MODE_PRIVATE)) {

        byte[] byteChunk = new byte[BUFFER_SIZE];
        int n;

        while ((n = is.read(byteChunk)) > 0) {
          fos.write(byteChunk, 0, n);
        }
      }
    } catch (IOException e) {
      throw new TileDownloadException("Failed to download tile", e);
    }
  }

  /** Update a tile's state in the database and initiate a download of the tile source file. */
  private Completable downloadTile(Tile tile) {
    Map<String, String> requestProperties = new HashMap<>();

    if (tile.getState() == State.IN_PROGRESS) {
      File existingTileFile = new File(context.getFilesDir(), tile.getPath());
      requestProperties.put("Range", existingTileFile.length() + "-");
    }

    return localDataStore
        .insertOrUpdateTile(tile.toBuilder().setState(Tile.State.IN_PROGRESS).build())
        .andThen(
          Completable.fromRunnable(
                () -> {
                  downloadTileFile(tile, requestProperties);
                }))
        .onErrorResumeNext(
            e -> {
              Timber.d(e, "Failed to download tile: %s", tile);
              return localDataStore.insertOrUpdateTile(
                  tile.toBuilder().setState(State.FAILED).build());
            })
        .andThen(
            localDataStore.insertOrUpdateTile(tile.toBuilder().setState(State.DOWNLOADED).build()));
  }

  /**
   * Verifies that {@param tile} marked as {@code Tile.State.DOWNLOADED} in the local database still
   * exists in the app's storage. If the tile's source file isn't present, initiates a download of
   * source file.
   */
  private Completable checkDownload(Tile tile) {
    File file = new File(context.getFilesDir(), tile.getPath());

    if (file.exists()) {
      return Completable.complete();
    }

    return downloadTile(tile);
  }

  private Completable processTiles(ImmutableList<Tile> pendingTiles) {
    return Observable.fromIterable(pendingTiles)
        .flatMapCompletable(
            t -> {
              switch (t.getState()) {
                case DOWNLOADED:
                  return checkDownload(t);
                case PENDING:
                case IN_PROGRESS:
                case FAILED:
                default:
                  return downloadTile(t);
              }
            });
  }

  /**
   * Given a tile identifier, downloads a tile source file and saves it to the app's file storage.
   * If the tile source file already exists on the device, this method returns {@code
   * Result.success()} and does not re-download the file.
   */
  @NonNull
  @Override
  public Result doWork() {
    ImmutableList<Tile> pendingTiles = localDataStore.getPendingTiles().blockingGet();

    // When there are no tiles in the db, the blockingGet returns null.
    // If that isn't the case, another worker may have already taken care of the work.
    // In this case, we return a result immediately to stop the worker.
    if (pendingTiles == null) {
      return Result.success();
    }

    Timber.d("Downloading tiles: %s", pendingTiles);

    try {
      processTiles(pendingTiles).blockingAwait();
      return Result.success();
    } catch (Throwable t) {
      Timber.d(t, "Downloads for tiles failed: %s", pendingTiles);
      return Result.failure();
    }
  }
}
