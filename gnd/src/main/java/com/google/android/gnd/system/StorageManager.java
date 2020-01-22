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
import android.util.Log;
import io.reactivex.Completable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StorageManager {

  public static final String TAG = StorageManager.class.getName();

  private static final int PICKFILE_REQUEST_CODE = StorageManager.class.hashCode() & 0xffff;
  private final PermissionsManager permissionsManager;
  private final ActivityStreams activityStreams;

  @Inject
  public StorageManager(PermissionsManager permissionsManager, ActivityStreams activityStreams) {
    this.permissionsManager = permissionsManager;
    this.activityStreams = activityStreams;
  }

  public void imagePicker() {
    permissionsManager
        .obtainPermission(permission.READ_EXTERNAL_STORAGE)
        .andThen(sendImagePickerIntent())
        .andThen(
            activityStreams
                .getNextActivityResult(PICKFILE_REQUEST_CODE)
                .doOnNext(
                    activityResult -> {
                      if (activityResult.isOk()) {
                        Intent intent = activityResult.getData();
                        if (intent != null) {
                          Log.d(TAG, activityResult.getData().getData() + " = uri");
                        }
                      } else if (activityResult.isCanceled()) {
                        Log.d(TAG, "file picker canceled");
                      }
                    }))
        .subscribe();
  }

  private Completable sendImagePickerIntent() {
    return Completable.fromAction(
        () ->
            activityStreams.withActivity(
                activity -> {
                  Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                  intent.setType("image/*");
                  activity.startActivityForResult(intent, PICKFILE_REQUEST_CODE);
                  Log.d(TAG, "file picker intent sent");
                }));
  }
}
