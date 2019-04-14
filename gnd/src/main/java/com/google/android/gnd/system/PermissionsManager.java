/*
 * Copyright 2019 Google LLC
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

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import com.google.android.gnd.rx.RxCompletable;
import io.reactivex.Completable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PermissionsManager {
  private static final String TAG = PermissionsManager.class.getSimpleName();
  private static final int PERMISSIONS_REQUEST_CODE = PermissionsManager.class.hashCode() & 0xffff;

  private final Context context;
  private final ActivityStreams activityStreams;

  @Inject
  public PermissionsManager(Application app, ActivityStreams activityStreams) {
    context = app.getApplicationContext();
    this.activityStreams = activityStreams;
  }

  public Completable obtainPermission(String permission) {
    return RxCompletable.completeIf(() -> requestPermission(permission))
        .ambWith(getPermissionsResult(permission));
  }

  private boolean requestPermission(String permission) {
    if (isGranted(permission)) {
      Log.d(TAG, permission + " already granted");
      return true;
    } else {
      Log.d(TAG, "Requesting " + permission);
      activityStreams.withActivity(
          activity ->
              ActivityCompat.requestPermissions(
                  activity, new String[] {permission}, PERMISSIONS_REQUEST_CODE));
      return false;
    }
  }

  private boolean isGranted(String permission) {
    return ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED;
  }

  private Completable getPermissionsResult(String permission) {
    return activityStreams
        .getNextRequestPermissionsResult(PERMISSIONS_REQUEST_CODE)
        .flatMapCompletable(
            r ->
                RxCompletable.completeOrError(
                    r.isGranted(permission), PermissionDeniedException.class));
  }

  public static class PermissionDeniedException extends Exception {}
}
