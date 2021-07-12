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

package com.google.android.gnd.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import com.google.android.gnd.Config;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.persistence.remote.RemoteStorageManager;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.rx.annotations.Cold;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Single;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Provides access to user-provided media stored locally and remotely. This currently includes only
 * photos.
 */
@Singleton
public class UserMediaRepository {

  private final Context context;
  private final RemoteStorageManager remoteStorageManager;
  private final OfflineUuidGenerator uuidGenerator;

  @Inject
  public UserMediaRepository(
      @ApplicationContext Context context,
      RemoteStorageManager remoteStorageManager,
      OfflineUuidGenerator uuidGenerator) {
    this.context = context;
    this.remoteStorageManager = remoteStorageManager;
    this.uuidGenerator = uuidGenerator;
  }

  private File getRootDir() {
    return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
  }

  public String createImageFilename(Field field) {
    return field.getId() + "-" + uuidGenerator.generateUuid() + Config.PHOTO_EXT;
  }

  public File createImageFile(Field field) {
    return new File(getRootDir(), createImageFilename(field));
  }

  /**
   * Creates a new file from bitmap and saves under external app directory.
   *
   * @throws IOException If path is not accessible or error occurs while saving file
   */
  public File savePhoto(Bitmap bitmap, Field field) throws IOException {
    File file = createImageFile(field);
    try (FileOutputStream fos = new FileOutputStream(file)) {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
    }
    Timber.d("Photo saved %s : %b", file.getPath(), file.exists());
    return file;
  }

  public void addImageToGallery(String filePath, String title) throws FileNotFoundException {
    MediaStore.Images.Media.insertImage(context.getContentResolver(), filePath, title, "");
  }

  /**
   * Attempts to load the file from local cache. Else attempts to fetch it from Firestore Storage.
   * Returns the uri of the file.
   *
   * @param path Final destination path of the uploaded file relative to Firestore
   */
  @Cold
  public Single<Uri> getDownloadUrl(String path) {
    return path.isEmpty() ? Single.just(Uri.EMPTY) : getFileUriFromRemotePath(path);
  }

  private Single<Uri> getFileUriFromRemotePath(String destinationPath) {
    File file = getLocalFileFromRemotePath(destinationPath);
    if (file.exists()) {
      return Single.fromCallable(() -> Uri.fromFile(file));
    } else {
      Timber.d("File doesn't exist locally: %s", file.getPath());
      return remoteStorageManager.getDownloadUrl(destinationPath);
    }
  }

  /**
   * Returns the path of the file saved in the sdcard used for uploading to the provided destination
   * path.
   */
  public File getLocalFileFromRemotePath(String destinationPath) {
    String[] splits = destinationPath.split("/");
    String filename = splits[splits.length - 1];
    File file = new File(getRootDir(), filename);
    if (!file.exists()) {
      Timber.e("File not found: %s", file.getPath());
    }
    return file;
  }
}
