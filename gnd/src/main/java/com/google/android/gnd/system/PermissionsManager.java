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

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PermissionsManager {
  private static final String TAG = PermissionsManager.class.getSimpleName();
  private static final int PERMISSIONS_REQUEST_CODE = PermissionsManager.class.hashCode() & 0xffff;

  private final Context context;
  private final Subject<PermissionsRequest> permissionsRequestSubject;
  private final Subject<PermissionsResult> permissionsResultSubject;

  @Inject
  public PermissionsManager(Application app) {
    permissionsRequestSubject = PublishSubject.create();
    permissionsResultSubject = PublishSubject.create();
    context = app.getApplicationContext();
  }

  public Observable<PermissionsRequest> permissionsRequests() {
    return permissionsRequestSubject;
  }

  public Completable obtainFineLocationPermission() {
    return obtainPermission(ACCESS_FINE_LOCATION);
  }

  /**
   * Callback for use from onRequestPermissionsResult() in Activity.
   */
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode != PERMISSIONS_REQUEST_CODE) {
      return;
    }
    for (int i = 0; i < permissions.length; i++) {
      permissionsResultSubject.onNext(new PermissionsResult(permissions[i], grantResults[i]));
    }
  }

  private Completable obtainPermission(String permission) {
    if (isGranted(permission)) {
      Log.d(TAG, permission + " already granted");
      return Completable.complete();
    }

    return Completable.create(source ->
        permissionsResultSubject
            .doOnSubscribe(__ -> requestPermission(permission))
            .filter(r -> r.getPermission().equals(permission))
            .subscribe(r -> {
              Log.d(TAG, r.toString());
              if (r.isGranted()) {
                source.onComplete();
              } else {
                source.onError(new PermissionDeniedException());
              }
            }));
  }

  private void requestPermission(String permission) {
    Log.i(TAG, "Requesting " + permission);
    permissionsRequestSubject
        .onNext(new PermissionsRequest(PERMISSIONS_REQUEST_CODE, new String[]{permission}));
  }

  private boolean isGranted(String permission) {
    return ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED;
  }

  public static class PermissionsRequest {
    private int requestCode;
    private String[] permissions;

    private PermissionsRequest(int requestCode, String[] permissions) {
      this.requestCode = requestCode;
      this.permissions = permissions;
    }

    public int getRequestCode() {
      return requestCode;
    }

    public String[] getPermissions() {
      return permissions;
    }
  }

  private static class PermissionsResult {
    private String permission;
    private int grantResult;

    private PermissionsResult(String permission, int grantResult) {
      this.permission = permission;
      this.grantResult = grantResult;
    }

    public String getPermission() {
      return permission;
    }

    boolean isGranted() {
      return grantResult == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public String toString() {
      return permission + " grant result: " + grantResult;
    }
  }

  public static class PermissionDeniedException extends Exception {
  }
}
