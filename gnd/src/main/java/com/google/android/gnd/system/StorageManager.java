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
import com.google.android.gnd.persistence.remote.RemoteStorageManager;
import com.google.android.gnd.persistence.sync.PhotoSyncWorkManager;
import com.google.android.gnd.rx.RxTask;
import com.google.android.gnd.system.ActivityStreams.ActivityResult;
import com.google.android.gnd.ui.util.FileUtil;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Manages permissions needed for accessing storage and related flows to/from Activity. */
@Singleton
public class StorageManager {

  private static final int PICK_PHOTO_REQUEST_CODE = StorageManager.class.hashCode() & 0xffff;
  private final Context context;
  private final PermissionsManager permissionsManager;
  private final ActivityStreams activityStreams;
  private final RemoteStorageManager remoteStorageManager;
  private final PhotoSyncWorkManager photoSyncWorkManager;
  private final FileUtil fileUtil;

  @Inject
  public StorageManager(
      Context context,
      PermissionsManager permissionsManager,
      ActivityStreams activityStreams,
      RemoteStorageManager remoteStorageManager,
      PhotoSyncWorkManager photoSyncWorkManager,
      FileUtil fileUtil) {
    this.context = context;
    this.permissionsManager = permissionsManager;
    this.activityStreams = activityStreams;
    this.remoteStorageManager = remoteStorageManager;
    this.photoSyncWorkManager = photoSyncWorkManager;
    this.fileUtil = fileUtil;
  }

  /**
   * Requests for selecting a photo from the storage, if necessary permissions are granted.
   * Otherwise, requests for the permissions and then sends out the request.
   */
  public Completable launchPhotoPicker(String id) {
    return permissionsManager
        .obtainPermission(permission.READ_EXTERNAL_STORAGE)
        .andThen(sendPhotoPickerIntent(id));
  }

  // TODO: Move UI-specific code to UI layer (Fragment or any related helper)
  /** Enqueue an intent for selecting a photo from the storage. */
  private Completable sendPhotoPickerIntent(String id) {
    return Completable.fromAction(
        () ->
            activityStreams.withActivity(
                activity -> {
                  Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                  intent.setType("image/*");
                  activity.startActivityForResult(intent, PICK_PHOTO_REQUEST_CODE);
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

  private Uri getFileUriFromDestinationPath(String destinationPath) throws FileNotFoundException {
    File file = getLocalFileFromDestinationPath(destinationPath);
    return Uri.fromFile(file);
  }

  /**
   * Fetch url for the image from Firestore Storage. If the remote image is not available then
   * search for the file locally and return its uri.
   *
   * @param destinationPath Final destination path of the uploaded photo relative to Firestore
   */
  public Single<Uri> getDownloadUrl(String destinationPath) {
    return RxTask.toSingle(() -> remoteStorageManager.getDownloadUrl(destinationPath))
        .onErrorReturn(throwable -> getFileUriFromDestinationPath(destinationPath));
  }

  /** Save a copy of bitmap locally and enqueue worker for uploading to remote storage. */
  public Completable savePhoto(Bitmap bitmap, String filename, String remotePath)
      throws IOException {
    // Make a local copy of the image
    File file = fileUtil.saveBitmap(bitmap, filename);

    // TODO: Upload photos while synchronizing data mutations.
    //  The current behavior causes the photo to be uploaded even before the form is saved.
    //  This leads to redundancy if the user decides not to save the form afterwards.

    // Schedule for remote upload
    return photoSyncWorkManager.enqueueSyncWorker(file.getPath(), remotePath);
  }
}
