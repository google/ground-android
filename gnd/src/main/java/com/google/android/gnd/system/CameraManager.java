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
import android.os.Bundle;
import android.provider.MediaStore;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.ActivityStreams.ActivityResult;
import io.reactivex.Completable;
import io.reactivex.Observable;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/** Manages permissions needed for using camera and related flows to/from Activity. */
@Singleton
public class CameraManager {

  /** Used to identify requests coming from this application. */
  private static final int CAPTURE_PHOTO_REQUEST_CODE = CameraManager.class.hashCode() & 0xffff;

  private final PermissionsManager permissionsManager;
  private final ActivityStreams activityStreams;

  @Inject
  public CameraManager(PermissionsManager permissionsManager, ActivityStreams activityStreams) {
    this.permissionsManager = permissionsManager;
    this.activityStreams = activityStreams;
  }

  /** Launches the system's photo capture flow, first obtaining permissions if necessary. */
  @Cold
  public Completable launchPhotoCapture() {
    return permissionsManager
        .obtainPermission(permission.WRITE_EXTERNAL_STORAGE)
        .andThen(permissionsManager.obtainPermission(permission.CAMERA))
        .andThen(sendCapturePhotoIntent());
  }

  /** Enqueue an intent for capturing a photo. */
  @Cold
  private Completable sendCapturePhotoIntent() {
    return Completable.fromAction(
        () ->
            activityStreams.withActivity(
                activity -> {
                  Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                  activity.startActivityForResult(cameraIntent, CAPTURE_PHOTO_REQUEST_CODE);
                  Timber.d("capture photo intent sent");
                }));
  }

  /** Emits the result of the photo capture request. */
  @Hot
  public Observable<Bitmap> capturePhotoResult() {
    return activityStreams
        .getNextActivityResult(CAPTURE_PHOTO_REQUEST_CODE)
        .flatMap(this::onCapturePhotoResult);
  }

  /** Extracts the bitmap from the result returned by the activity, if present. */
  @Cold
  private Observable<Bitmap> onCapturePhotoResult(ActivityResult result) {
    // TODO: Investigate if returning a Maybe is better or not?
    return Observable.create(
        em -> {
          if (!result.isOk()) {
            // TODO(#726): Call onError()?
            return;
          }
          Intent data = result.getData();
          if (data == null) {
            // TODO(#726): Call onError()?
            return;
          }
          Bundle extras = data.getExtras();
          if (extras == null) {
            // TODO(#726): Call onError()?
            return;
          }
          em.onNext((Bitmap) extras.get("data"));
        });
  }
}
