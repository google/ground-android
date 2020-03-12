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
import android.provider.MediaStore.Images.Media;
import android.widget.ImageView;
import com.google.android.gnd.R;
import com.google.android.gnd.persistence.remote.FirestoreStorageManager;
import com.google.android.gnd.system.ActivityStreams.ActivityResult;
import com.google.android.gnd.ui.util.FileUtil;
import com.squareup.picasso.Picasso;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.io.File;
import java.io.FileNotFoundException;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/** Manages permissions needed for accessing storage and related flows to/from Activity. */
@Singleton
public class StorageManager {

  private static final int PICK_PHOTO_REQUEST_CODE = StorageManager.class.hashCode() & 0xffff;
  private final Context context;
  private final PermissionsManager permissionsManager;
  private final ActivityStreams activityStreams;
  private final FirestoreStorageManager firestoreStorageManager;
  private final FileUtil fileUtil;

  @Inject
  public StorageManager(
      Context context,
      PermissionsManager permissionsManager,
      ActivityStreams activityStreams,
      FirestoreStorageManager firestoreStorageManager,
      FileUtil fileUtil) {
    this.context = context;
    this.permissionsManager = permissionsManager;
    this.activityStreams = activityStreams;
    this.firestoreStorageManager = firestoreStorageManager;
    this.fileUtil = fileUtil;
  }

  /**
   * Requests for selecting a photo from the storage, if necessary permissions are granted.
   * Otherwise, requests for the permissions and then sends out the request.
   */
  public Completable launchPhotoPicker() {
    return permissionsManager
        .obtainPermission(permission.READ_EXTERNAL_STORAGE)
        .andThen(sendPhotoPickerIntent());
  }

  /** Enqueue an intent for selecting a photo from the storage. */
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
  public Observable<Bitmap> photoPickerResult() {
    return activityStreams
        .getNextActivityResult(PICK_PHOTO_REQUEST_CODE)
        .flatMap(this::onPickPhotoResult)
        .map(uri -> Media.getBitmap(context.getContentResolver(), uri));
  }

  /** Fetch Uri from the result, if present. */
  // TODO: Investigate if returning a Maybe is better or not?
  private Observable<Uri> onPickPhotoResult(ActivityResult result) {
    return Observable.create(
        em -> {
          if (!result.isOk()) {
            return;
          }
          Intent data = result.getData();
          if (data == null) {
            return;
          }
          em.onNext(data.getData());
        });
  }

  /**
   * Returns the path of the file saved in the sdcard used for uploading to the provided destination
   * path.
   */
  private File getLocalFileFromDestinationPath(String destinationPath)
      throws FileNotFoundException {
    String[] splits = destinationPath.split("/");
    return fileUtil.getFile(splits[splits.length - 1]);
  }

  /**
   * Load photo from the provided destination path.
   *
   * <p>If the image is not uploaded yet, then parse the filename from path and load the file from
   * local storage.
   *
   * @param imageView Placeholder for photo field
   * @param destinationPath Destination path of the uploaded photo
   */
  public void loadPhotoFromDestinationPath(ImageView imageView, String destinationPath) {
    firestoreStorageManager
        .getDownloadUrl(destinationPath)
        .doOnSuccess(
            uri -> {
              // Load the file from Firestore Storage
              Picasso.get()
                  .load(uri)
                  .placeholder(R.drawable.ic_photo_grey_600_24dp)
                  .into(imageView);
            })
        .doOnError(
            throwable -> {
              // Load file locally
              File file = getLocalFileFromDestinationPath(destinationPath);
              Picasso.get()
                  .load(file)
                  .placeholder(R.drawable.ic_photo_grey_600_24dp)
                  .into(imageView);
            })
        .subscribe();
  }
}
