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

package com.google.android.gnd.persistence.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.android.gnd.R;
import com.google.android.gnd.model.basemap.tile.TileSource;
import com.google.android.gnd.model.basemap.tile.TileSource.State;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.remote.TransferProgress;
import com.google.android.gnd.system.NotificationManager;
import com.google.common.collect.ImmutableList;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
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
@HiltWorker
public class TileSourceDownloadWorker extends BaseWorker {
  private static final int BUFFER_SIZE = 4096;

  private final Context context;
  private final LocalDataStore localDataStore;

  @AssistedInject
  public TileSourceDownloadWorker(
      @Assisted @NonNull Context context,
      @Assisted @NonNull WorkerParameters params,
      LocalDataStore localDataStore,
      NotificationManager notificationManager) {
    super(context, params, notificationManager, TileSourceDownloadWorker.class.hashCode());
    this.context = context;
    this.localDataStore = localDataStore;
  }

  /**
   * Given a tile, downloads the given {@param tile}'s source file and saves it to the device's app
   * storage. Optional HTTP request header {@param requestProperties} may be provided.
   */
  private void downloadTileFile(TileSource tileSource, Map<String, String> requestProperties)
      throws TileSourceDownloadException {

    int mode = Context.MODE_PRIVATE;

    try {
      URL url = new URL(tileSource.getUrl());
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      if (!requestProperties.isEmpty()) {
        for (Map.Entry<String, String> property : requestProperties.entrySet()) {
          connection.setRequestProperty(property.getKey(), property.getValue());
        }
        mode = Context.MODE_APPEND;
      }

      connection.connect();

      try (InputStream is = connection.getInputStream();
          FileOutputStream fos = context.openFileOutput(tileSource.getPath(), mode)) {

        byte[] byteChunk = new byte[BUFFER_SIZE];
        int n;

        while ((n = is.read(byteChunk)) > 0) {
          fos.write(byteChunk, 0, n);
        }
      }
    } catch (IOException e) {
      throw new TileSourceDownloadException("Failed to download tile", e);
    }
  }

  /** Update a tile's state in the database and initiate a download of the tile source file. */
  private Completable downloadTileSource(TileSource tileSource) {
    Map<String, String> requestProperties = new HashMap<>();

    // To resume a download for an in progress tile, we use the HTTP Range request property.
    // The range property takes a range of bytes, the server returns the content of the resource
    // that corresponds to the given byte range.
    //
    // To resume a download, we get the current length, in bytes, of the file on disk.
    // appending '-' to the byte value tells the server to return the range of bytes from the given
    // byte value to the end of the file, e.g. '500-' returns contents starting at byte 500 to EOF.
    //
    // Note that length returns 0 when the file does not exist, so this correctly handles an edge
    // case whereby the local DB has a tile state of IN_PROGRESS but none of the file has been
    // downloaded yet (since then we'll fetch the range '0-', the entire file).
    //
    // For more info see: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range
    if (tileSource.getState() == State.IN_PROGRESS) {
      File existingTileFile = new File(context.getFilesDir(), tileSource.getPath());
      requestProperties.put("Range", "bytes=" + existingTileFile.length() + "-");
    }

    return localDataStore
        .insertOrUpdateTileSource(
            tileSource.toBuilder().setState(TileSource.State.IN_PROGRESS).build())
        .andThen(
            Completable.fromRunnable(
                () -> {
                  downloadTileFile(tileSource, requestProperties);
                }))
        .onErrorResumeNext(
            e -> {
              Timber.d(e, "Failed to download tile: %s", tileSource);
              return localDataStore.insertOrUpdateTileSource(
                  tileSource.toBuilder().setState(State.FAILED).build());
            })
        .andThen(
            localDataStore.insertOrUpdateTileSource(
                tileSource.toBuilder().setState(State.DOWNLOADED).build()));
  }

  /**
   * Verifies that {@param tile} marked as {@code Tile.State.DOWNLOADED} in the local database still
   * exists in the app's storage. If the tile's source file isn't present, initiates a download of
   * source file.
   */
  private Completable downloadIfNotFound(TileSource tileSource) {
    File file = new File(context.getFilesDir(), tileSource.getPath());

    if (file.exists()) {
      return Completable.complete();
    }

    return downloadTileSource(tileSource);
  }

  private Completable processTileSources(ImmutableList<TileSource> pendingTileSources) {
    return Observable.fromIterable(pendingTileSources)
        .doOnNext(
            tile ->
                sendNotification(
                    TransferProgress.inProgress(
                        pendingTileSources.size(), pendingTileSources.indexOf(tile) + 1)))
        .flatMapCompletable(
            t -> {
              switch (t.getState()) {
                case DOWNLOADED:
                  return downloadIfNotFound(t);
                case PENDING:
                case IN_PROGRESS:
                case FAILED:
                default:
                  return downloadTileSource(t);
              }
            })
        .compose(this::notifyTransferState);
  }

  /**
   * Given a tile identifier, downloads a tile source file and saves it to the app's file storage.
   * If the tile source file already exists on the device, this method returns {@code
   * Result.success()} and does not re-download the file.
   */
  @NonNull
  @Override
  public Result doWork() {
    ImmutableList<TileSource> pendingTileSources =
        localDataStore.getPendingTileSources().blockingGet();

    // When there are no tiles in the db, the blockingGet returns null.
    // If that isn't the case, another worker may have already taken care of the work.
    // In this case, we return a result immediately to stop the worker.
    if (pendingTileSources == null) {
      return Result.success();
    }

    Timber.d("Downloading tiles: %s", pendingTileSources);

    try {
      processTileSources(pendingTileSources).blockingAwait();
      return Result.success();
    } catch (Throwable t) {
      Timber.d(t, "Downloads for tiles failed: %s", pendingTileSources);
      return Result.failure();
    }
  }

  @Override
  public String getNotificationTitle() {
    return getApplicationContext().getString(R.string.downloading_tiles);
  }

  static class TileSourceDownloadException extends RuntimeException {
    TileSourceDownloadException(String msg, Throwable e) {
      super(msg, e);
    }
  }
}
