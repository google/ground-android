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

public class TileRemovalWorker extends Worker {
  private static final String TAG = FileDownloadWorker.class.getSimpleName();

  private static final String TILE_ID = "tile_id";

  private final Context context;
  private final LocalDataStore localDataStore;
  private final String tileId;

  public TileRemovalWorker(
      @NonNull Context context, @NonNull WorkerParameters params, LocalDataStore localDataStore) {
    super(context, params);
    this.context = context;
    this.localDataStore = localDataStore;
    this.tileId = params.getInputData().getString(TILE_ID);
  }

  /** Creates input data for the FileDownloadWorker. */
  public static Data createInputData(String tilePrimaryKey) {
    return new Data.Builder().putString(TILE_ID, tilePrimaryKey).build();
  }

  /**
   * Given a tile, deletes a tile source file from the device's app storage.
   *
   * @param tile
   * @return
   */
  private Result removeTile(Tile tile) {
    localDataStore
        .insertOrUpdateTile(tile.toBuilder().setState(Tile.State.REMOVED).build())
        .blockingAwait();
    File tileFile = new File(context.getFilesDir(), Tile.pathFromId(tile.getId()));

    if (tileFile.delete()) {
      Log.d(TAG, "Tile file" + tileFile.getPath() + "deleted");
      return Result.success();
    }

    return Result.failure();
  }

  /**
   * Given a tile identifier, deletes a tile source file from the app's file storage, then removes
   * the corresponding {@code TileEntity} from the local data store.
   */
  @NonNull
  @Override
  public Result doWork() {
    Log.d(TAG, "Removing tile: " + Tile.pathFromId(tileId));
    Tile tile = localDataStore.getTile(tileId).blockingGet();

    // When there is no tile in the db, the maybe completes and returns null.
    // There's nothing else for us to do in this case.
    if (tile == null) {
      Log.d(TAG, "Tile does not exist in the data store");
      return Result.failure();
    }

    switch (tile.getState()) {
      case DOWNLOADED:
        return removeTile(tile);
      case PENDING:
        return removeTile(tile);
      case FAILED:
        return removeTile(tile);
      case IN_PROGRESS:
        return removeTile(tile);
      default:
        return Result.success();
    }
  }
}
