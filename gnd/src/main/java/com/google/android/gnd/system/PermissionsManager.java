/*
 * Copyright 2018 Google LLC
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
import com.google.android.gnd.rx.RxCompletable;
import com.google.android.gnd.rx.annotations.Cold;
import io.reactivex.Completable;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/** Provides access to obtain and check the app's permissions. */
@Singleton
public class PermissionsManager {
  private static final int PERMISSIONS_REQUEST_CODE = PermissionsManager.class.hashCode() & 0xffff;

  private final Context context;
  private final ActivityStreams activityStreams;

  @Inject
  public PermissionsManager(Application app, ActivityStreams activityStreams) {
    this.context = app.getApplicationContext();
    this.activityStreams = activityStreams;
  }

  /**
   * Asynchronously requests the app be granted the specified permission. If the permission has
   * already been granted, completes immediately, otherwise completes once the next permissions
   * result is received.
   */
  @Cold
  public Completable obtainPermission(String permission) {
    return RxCompletable.completeIf(() -> requestPermission(permission))
        .ambWith(getPermissionsResult(permission));
  }

  /**
   * Sends the system request that the app be granted the specified permission. Returns {@code true}
   * if the permission was already granted.
   */
  private boolean requestPermission(String permission) {
    if (isGranted(permission)) {
      Timber.d("%s already granted", permission);
      return true;
    } else {
      Timber.d("Requesting %s", permission);
      activityStreams.withActivity(
          activity ->
              ActivityCompat.requestPermissions(
                  activity, new String[] {permission}, PERMISSIONS_REQUEST_CODE));
      return false;
    }
  }

  /** Returns {@code true} iff the app has been granted the specified permission. */
  private boolean isGranted(String permission) {
    return ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED;
  }

  /**
   * Returns a {@link Completable} that completes once the specified permission is granted or
   * terminates with error {@link PermissionDeniedException} if the requested permission was denied.
   */
  @Cold
  private Completable getPermissionsResult(String permission) {
    return activityStreams
        .getNextRequestPermissionsResult(PERMISSIONS_REQUEST_CODE)
        .flatMapCompletable(
            r ->
                RxCompletable.completeOrError(
                    () -> r.isGranted(permission), PermissionDeniedException.class));
  }

  /**
   * Indicates a request to grant the app permissions was denied. More specifically, it indicates
   * the requested permission was not in the list of granted permissions in the system's response.
   */
  public static class PermissionDeniedException extends Exception {}
}
