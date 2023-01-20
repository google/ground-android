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
package com.google.android.ground.persistence.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.common.collect.ImmutableList
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Completable
import io.reactivex.Observable
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import timber.log.Timber

/**
 * A worker that downloads files to the device in the background. The target URL and file name are
 * provided in a [Data] object. This worker should only run when the device has a network
 * connection.
 */
@HiltWorker
class TileSetDownloadWorker
@AssistedInject
constructor(
  @param:Assisted private val context: Context,
  @Assisted params: WorkerParameters,
  private val localDataStore: LocalDataStore
) : Worker(context, params) {

  /**
   * Given a tile, downloads the given {@param tile}'s source file and saves it to the device's app
   * storage. Optional HTTP request header {@param requestProperties} may be provided.
   */
  @Throws(TileSetDownloadException::class)
  private fun downloadTileFile(tileSet: TileSet, requestProperties: Map<String, String>) {
    var mode = Context.MODE_PRIVATE

    try {
      val url = URL(tileSet.url)
      val connection = url.openConnection() as HttpURLConnection

      if (requestProperties.isNotEmpty()) {
        for ((key, value) in requestProperties) {
          connection.setRequestProperty(key, value)
        }
        mode = Context.MODE_APPEND
      }

      connection.connect()

      connection.inputStream.use { inputStream ->
        context.openFileOutput(tileSet.path, mode).use { fos ->
          val byteChunk = ByteArray(BUFFER_SIZE)
          var n: Int

          while (inputStream.read(byteChunk).also { n = it } > 0) {
            fos.write(byteChunk, 0, n)
          }
        }
      }
    } catch (e: IOException) {
      throw TileSetDownloadException("Failed to download tile", e)
    }
  }

  /** Update a tile's state in the database and initiate a download of the tile source file. */
  private fun downloadTileSet(tileSet: TileSet): Completable {
    val requestProperties: MutableMap<String, String> = HashMap()

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
    if (tileSet.state === TileSet.State.IN_PROGRESS) {
      val existingTileFile = File(context.filesDir, tileSet.path)
      requestProperties["Range"] = "bytes=" + existingTileFile.length() + "-"
    }
    return localDataStore.tileSetStore
      .insertOrUpdateTileSet(
        tileSet.copy(
          state = TileSet.State.IN_PROGRESS,
        )
      )
      .andThen(Completable.fromRunnable { downloadTileFile(tileSet, requestProperties) })
      .onErrorResumeNext { e ->
        Timber.d(e, "Failed to download tile: $tileSet")
        localDataStore.tileSetStore.insertOrUpdateTileSet(
          tileSet.copy(
            state = TileSet.State.FAILED,
          )
        )
      }
      .andThen(
        localDataStore.tileSetStore.insertOrUpdateTileSet(
          tileSet.copy(
            state = TileSet.State.DOWNLOADED,
          )
        )
      )
  }

  /**
   * Verifies that {@param tile} marked as `Tile.State.DOWNLOADED` in the local database still
   * exists in the app's storage. If the tile's source file isn't present, initiates a download of
   * source file.
   */
  private fun downloadIfNotFound(tileSet: TileSet): Completable {
    val file = File(context.filesDir, tileSet.path)
    return if (file.exists()) {
      Completable.complete()
    } else downloadTileSet(tileSet)
  }

  private fun processTileSets(pendingTileSets: ImmutableList<TileSet>): Completable =
    Observable.fromIterable(pendingTileSets).flatMapCompletable { tileSet ->
      when (tileSet.state) {
        TileSet.State.DOWNLOADED -> downloadIfNotFound(tileSet)
        TileSet.State.PENDING,
        TileSet.State.IN_PROGRESS,
        TileSet.State.FAILED -> downloadTileSet(tileSet)
      }
    }

  /**
   * Given a tile identifier, downloads a tile source file and saves it to the app's file storage.
   * If the tile source file already exists on the device, this method returns `Result.success()`
   * and does not re-download the file.
   */
  override fun doWork(): Result {
    val pendingTileSets =
      localDataStore.tileSetStore.pendingTileSets.blockingGet() ?: return Result.success()

    // When there are no tiles in the db, the blockingGet returns null.
    // If that isn't the case, another worker may have already taken care of the work.
    // In this case, we return a result immediately to stop the worker.
    Timber.d("Downloading tiles: $pendingTileSets")
    return try {
      processTileSets(pendingTileSets).blockingAwait()
      Result.success()
    } catch (t: Throwable) {
      Timber.d(t, "Downloads for tiles failed: $pendingTileSets")
      Result.failure()
    }
  }

  internal class TileSetDownloadException(msg: String?, e: Throwable?) : RuntimeException(msg, e)
  companion object {
    private const val BUFFER_SIZE = 4096
  }
}
