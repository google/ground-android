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

package com.google.android.gnd.system;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.util.BitmapUtil;
import com.google.android.gnd.ui.util.FileUtil;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/** Manages permissions needed for accessing storage and related flows to/from Activity. */
@Singleton
public class StorageManager {

  static final int PICK_PHOTO_REQUEST_CODE = StorageManager.class.hashCode() & 0xffff;

  private final Context context;
  private final PermissionsManager permissionsManager;
  private final ActivityStreams activityStreams;
  private final FileUtil fileUtil;
  private final BitmapUtil bitmapUtil;

  @Inject
  public StorageManager(
      @ApplicationContext Context context,
      PermissionsManager permissionsManager,
      ActivityStreams activityStreams,
      FileUtil fileUtil,
      BitmapUtil bitmapUtil) {
    this.context = context;
    this.permissionsManager = permissionsManager;
    this.activityStreams = activityStreams;
    this.fileUtil = fileUtil;
    this.bitmapUtil = bitmapUtil;
  }

  /**
   * Requests for selecting a photo from the storage, if necessary permissions are granted.
   * Otherwise, requests for the permissions and then sends out the request.
   */
  @Cold
  public Maybe<Bitmap> selectPhoto() {
    return permissionsManager
        .obtainPermission(permission.READ_EXTERNAL_STORAGE)
        .andThen(sendPhotoPickerIntent())
        .andThen(photoPickerResult());
  }

  // TODO: Move UI-specific code to UI layer (Fragment or any related helper)
  /** Enqueue an intent for selecting a photo from the storage. */
  @Cold
  private Completable sendPhotoPickerIntent() {
    return Completable.fromAction(
        () ->
            activityStreams.withActivity(
                activity -> {
                  Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                  intent.setType("image/*");
                  activity.startActivityForResult(intent, PICK_PHOTO_REQUEST_CODE);
                  Timber.d("file picker intent sent");
                }));
  }

  /** Observe for the result of request code {@link StorageManager#PICK_PHOTO_REQUEST_CODE}. */
  @Hot(terminates = true)
  Maybe<Bitmap> photoPickerResult() {
    return activityStreams
        .getNextActivityResult(PICK_PHOTO_REQUEST_CODE)
        .flatMapMaybe(this::onPickPhotoResult)
        .map(bitmapUtil::fromUri)
        .singleElement();
  }

  private Optional<Uri> parseResult(@Nullable Intent intent) {
    if (intent == null || intent.getData() == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(intent.getData());
  }

  /** Fetch Uri from the result, if present. */
  @Cold
  private Maybe<Uri> onPickPhotoResult(ActivityResult result) {
    return Maybe.create(
        emitter -> {
          if (result.isOk()) {
            emitter.onSuccess(parseResult(result.getData()).orElseThrow());
          } else {
            emitter.onComplete();
          }
        });
  }

  /**
   * Save a copy of bitmap locally.
   *
   * @param bitmap The contents of the image to save.
   * @param filename The filename to use. May not contain path separators.
   */
  @Cold
  public Completable savePhoto(Bitmap bitmap, String filename) {
    try {
      addImageToGallery(bitmap, filename);
      File file = fileUtil.saveBitmap(bitmap, filename);
      Timber.d("Photo saved %s : %b", filename, file.exists());
      return Completable.complete();
    } catch (IOException e) {
      return Completable.error(e);
    }
  }

  public void addImageToGallery(String filePath, String title) throws FileNotFoundException {
    MediaStore.Images.Media.insertImage(context.getContentResolver(), filePath, title, "");
  }

  public void addImageToGallery(Bitmap bitmap, String title) {
    MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, title, "");
  }
}
