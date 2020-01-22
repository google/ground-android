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
import android.util.Log;
import io.reactivex.Completable;
import javax.inject.Inject;
import javax.inject.Singleton;

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

  public void clickPhoto() {
    permissionsManager
        .obtainPermission(permission.WRITE_EXTERNAL_STORAGE)
        .andThen(permissionsManager.obtainPermission(permission.CAMERA))
        .andThen(sendCaptureImageIntent())
        .andThen(
            activityStreams
                .getNextActivityResult(CAPTURE_PHOTO_REQUEST_CODE)
                .doOnNext(
                    activityResult -> {
                      if (activityResult.isOk()) {
                        Intent intent = activityResult.getData();
                        if (intent != null) {
                          Bitmap photo = (Bitmap) intent.getExtras().get("data");
                          Log.d(TAG, photo.toString());
                        }
                      } else if (activityResult.isCanceled()) {
                        Log.d(TAG, "capture photo canceled");
                      }
                    }))
        .subscribe();
  }

  private Completable sendCaptureImageIntent() {
    return Completable.fromAction(
        () ->
            activityStreams.withActivity(
                activity -> {
                  Intent cameraIntent =
                      new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                  activity.startActivityForResult(cameraIntent, CAPTURE_PHOTO_REQUEST_CODE);
                  Log.d(TAG, "capture photo intent sent");
                }));
  }
}
