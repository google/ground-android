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
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;
import com.google.android.gnd.system.ActivityStreams.ActivityResult;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Manages permissions needed for using camera and related flows to/from Activity. */
@Singleton
public class CameraManager {

  public static final String TAG = CameraManager.class.getName();

  private static final int CAPTURE_PHOTO_REQUEST_CODE = CameraManager.class.hashCode() & 0xffff;
  private final PermissionsManager permissionsManager;
  private final ActivityStreams activityStreams;

  @Inject
  public CameraManager(PermissionsManager permissionsManager, ActivityStreams activityStreams) {
    this.permissionsManager = permissionsManager;
    this.activityStreams = activityStreams;
  }

  public Completable launchImageCapture() {
    return permissionsManager
        .obtainPermission(permission.WRITE_EXTERNAL_STORAGE)
        .andThen(permissionsManager.obtainPermission(permission.CAMERA))
        .andThen(sendCaptureImageIntent());
  }

  public Observable<Optional<Bitmap>> captureImageResult() {
    return activityStreams
        .getNextActivityResult(CAPTURE_PHOTO_REQUEST_CODE)
        .filter(ActivityResult::isOk)
        .map(
            activityResult ->
                Optional.ofNullable(activityResult.getData())
                    .map(Intent::getExtras)
                    .map(extras -> (Bitmap) extras.get("data")));
  }

  private Completable sendCaptureImageIntent() {
    return Completable.fromAction(
        () ->
            activityStreams.withActivity(
                activity -> {
                  Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                  activity.startActivityForResult(cameraIntent, CAPTURE_PHOTO_REQUEST_CODE);
                  Log.d(TAG, "capture photo intent sent");
                }));
  }
}
