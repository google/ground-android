/*
 * Copyright 2021 Google LLC
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
package com.google.android.gnd.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.google.android.gnd.Config
import com.google.android.gnd.persistence.remote.RemoteStorageManager
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator
import com.google.android.gnd.rx.annotations.Cold
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to user-provided media stored locally and remotely. This currently includes only
 * photos.
 */
@Singleton
class UserMediaRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val remoteStorageManager: RemoteStorageManager,
    private val uuidGenerator: OfflineUuidGenerator
) {

    private val rootDir: File?
        get() = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    private fun createImageFilename(fieldId: String): String =
        fieldId + "-" + uuidGenerator.generateUuid() + Config.PHOTO_EXT

    fun createImageFile(fieldId: String): File = File(rootDir, createImageFilename(fieldId))

    /**
     * Creates a new file from bitmap and saves under external app directory.
     *
     * @throws IOException If path is not accessible or error occurs while saving file
     */
    @Throws(IOException::class)
    fun savePhoto(bitmap: Bitmap, fieldId: String): File =
        createImageFile(fieldId).apply {
            FileOutputStream(this).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }
            Timber.d("Photo saved %s : %b", path, exists())
        }

    @Throws(FileNotFoundException::class)
    fun addImageToGallery(filePath: String, title: String): String =
        MediaStore.Images.Media.insertImage(context.contentResolver, filePath, title, "")

    /**
     * Attempts to load the file from local cache. Else attempts to fetch it from Firestore Storage.
     * Returns the uri of the file.
     *
     * @param path Final destination path of the uploaded file relative to Firestore
     */
    fun getDownloadUrl(path: String): @Cold Single<Uri> =
        if (path.isEmpty()) Single.just(Uri.EMPTY)
        else getFileUriFromRemotePath(path)

    private fun getFileUriFromRemotePath(destinationPath: String): Single<Uri> {
        val file = getLocalFileFromRemotePath(destinationPath)
        return if (file.exists()) {
            Single.fromCallable { Uri.fromFile(file) }
        } else {
            Timber.d("File doesn't exist locally: %s", file.path)
            remoteStorageManager.getDownloadUrl(destinationPath)
        }
    }

    /**
     * Returns the path of the file saved in the sdcard used for uploading to the provided destination
     * path.
     */
    fun getLocalFileFromRemotePath(destinationPath: String): File {
        val filename = destinationPath.split('/').last()
        val file = File(rootDir, filename)
        if (!file.exists()) {
            Timber.e("File not found: %s", file.path)
        }
        return file
    }
}