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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.persistence.local.LocalDataStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java8.util.Optional;

/**
 * A worker that downloads files to the device in the background. The target URL and file name are
 * provided in a {@link Data} object. This worker should only run when the device has a network
 * connection.
 */
public class TileDownloadWorker extends Worker {
  private static final String TAG = TileDownloadWorker.class.getSimpleName();

  private static final String TILE_ID = "tile_id";
  private static final int BUFFER_SIZE = 4096;

  private final Context context;
  private final LocalDataStore localDataStore;
  private final String tileId;

  public TileDownloadWorker(
      @NonNull Context context, @NonNull WorkerParameters params, LocalDataStore localDataStore) {
    super(context, params);
    this.context = context;
    this.localDataStore = localDataStore;
    this.tileId = params.getInputData().getString(TILE_ID);
  }

  /** Creates input data for the TileDownloadWorker. */
  public static Data createInputData(String tilePrimaryKey) {
    return new Data.Builder().putString(TILE_ID, tilePrimaryKey).build();
  }

  /**
   * Given a tile, downloads the given {@param tile}'s source file and saves it to the device's app
   * storage. Optional HTTP request header {@param requestProperties} may be provided.
   */
  private Result downloadTileFile(Tile tile, Optional<HashMap<String, String>> requestProperties) {
    try {
      URL url = new URL(tile.getUrl());
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      if (requestProperties.isPresent()) {
        for (Map.Entry<String, String> property : requestProperties.get().entrySet()) {
          connection.setRequestProperty(property.getKey(), property.getValue());
        }
      }

      connection.connect();

      InputStream is = connection.getInputStream();
      FileOutputStream fos = context.openFileOutput(tile.getPath(), Context.MODE_PRIVATE);
      byte[] byteChunk = new byte[BUFFER_SIZE];
      int n;

      while ((n = is.read(byteChunk)) > 0) {
        fos.write(byteChunk, 0, n);
      }

      is.close();
      fos.close();

      localDataStore
          .insertOrUpdateTile(tile.toBuilder().setState(Tile.State.DOWNLOADED).build())
          .blockingAwait();

      return Result.success();

    } catch (IOException e) {
      Log.d(TAG, "Failed to download and write file.", e);

      localDataStore
          .insertOrUpdateTile(tile.toBuilder().setState(Tile.State.FAILED).build())
          .blockingAwait();

      return Result.failure();
    }
  }

  /** Update a tile's state in the database and initiate a download of the tile source file. */
  private Result downloadTile(Tile tile) {
    localDataStore
        .insertOrUpdateTile(tile.toBuilder().setState(Tile.State.IN_PROGRESS).build())
        .blockingAwait();

    return downloadTileFile(tile, Optional.empty());
  }

  /** Resumes downloading the source for {@param tile} marked as {@code Tile.State.IN_PROGRESS}. */
  private Result resumeTileDownload(Tile tile) {
    File existingTileFile = new File(context.getFilesDir(), tile.getPath());
    HashMap<String, String> requestProperties = new HashMap<>();

    requestProperties.put("Range", existingTileFile.length() + "-");

    return downloadTileFile(tile, Optional.of(requestProperties));
  }

  /**
   * Verifies that {@param tile} marked as {@code Tile.State.DOWNLOADED} in the local database still
   * exists in the app's storage. If the tile's source file isn't present, initiates a download of
   * source file.
   */
  private Result checkDownload(Tile tile) {
    File file = new File(context.getFilesDir(), tile.getPath());

    if (file.exists()) {
      return Result.success();
    }

    return downloadTile(tile);
  }

  /**
   * Given a tile identifier, downloads a tile source file and saves it to the app's file storage.
   * If the tile source file already exists on the device, this method returns {@code
   * Result.success()} and does not re-download the file.
   */
  @NonNull
  @Override
  public Result doWork() {
    Tile tile = localDataStore.getTile(tileId).blockingGet();

    // When there is no tile in the db, the Maybe completes and returns null.
    // We expect tiles to be added to the DB prior to downloading.
    // If that isn't the case, we fail.
    if (tile == null) {
      return Result.failure();
    }

    Log.d(TAG, "Downloading tile: " + tile.getPath());

    switch (tile.getState()) {
      case DOWNLOADED:
        return checkDownload(tile);
      case PENDING:
      case FAILED:
        return downloadTile(tile);
      case IN_PROGRESS:
        return resumeTileDownload(tile);
      default:
        return Result.failure();
    }
  }
}
