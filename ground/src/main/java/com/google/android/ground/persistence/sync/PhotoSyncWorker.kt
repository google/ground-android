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
package com.google.android.ground.persistence.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.ground.persistence.remote.RemoteStorageManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileNotFoundException
import timber.log.Timber

/**
 * A worker that uploads photos from submissions to the FirestoreStorage in the background. The
 * source file and remote destination path are provided in a [Data] object. This worker should only
 * run when the device has a network connection.
 */
@HiltWorker
class PhotoSyncWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted workerParams: WorkerParameters,
  private val remoteStorageManager: RemoteStorageManager
) : Worker(context, workerParams) {

  private val localSourcePath: String =
    workerParams.inputData.getString(SOURCE_FILE_PATH_PARAM_KEY)!!
  private val remoteDestinationPath: String =
    workerParams.inputData.getString(DESTINATION_PATH_PARAM_KEY)!!

  override fun doWork(): Result {
    Timber.d("Attempting photo sync: $localSourcePath, $remoteDestinationPath")
    val file = File(localSourcePath)
    return if (file.exists()) {
      Timber.d("Starting photo upload: $localSourcePath, $remoteDestinationPath")
      try {
        remoteStorageManager.uploadMediaFromFile(file, remoteDestinationPath).blockingAwait()
        Result.success()
      } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().log("Photo sync failed")
        FirebaseCrashlytics.getInstance().recordException(e)
        Timber.e(e, "Photo sync failed: $localSourcePath, $remoteDestinationPath")
        Result.retry()
      }
    } else {
      FirebaseCrashlytics.getInstance().log("Photo missing on local device")
      FirebaseCrashlytics.getInstance().recordException(FileNotFoundException())
      Timber.e("Photo not found $localSourcePath, $remoteDestinationPath")
      Result.failure()
    }
  }

  companion object {
    private const val SOURCE_FILE_PATH_PARAM_KEY = "sourceFilePath"
    private const val DESTINATION_PATH_PARAM_KEY = "destinationPath"

    @JvmStatic
    fun createInputData(sourceFilePath: String, destinationPath: String): Data =
      Data.Builder()
        .putString(SOURCE_FILE_PATH_PARAM_KEY, sourceFilePath)
        .putString(DESTINATION_PATH_PARAM_KEY, destinationPath)
        .build()
  }
}
