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

package com.google.gnd.system;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;

import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class PermissionManager implements ActivityCompat.OnRequestPermissionsResultCallback {
  private static final String[] FINE_LOCATION_PERMISSIONS = {ACCESS_FINE_LOCATION};
  public static final int FINE_LOCATION_PERMISSIONS_REQUEST_CODE =
      FINE_LOCATION_PERMISSIONS.hashCode() & 0xffff;

  private final Activity activity;
  private Map<Integer, Pair<Runnable, Runnable>> callbacks; // Lower 16 bits reserved.

  public PermissionManager(Activity activity) {
    this.activity = activity;
    this.callbacks = new HashMap<>();
  }

  private boolean isFineLocationPermissionGranted() {
    return ContextCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED;
  }

  public void obtainFineLocationPermission(Runnable onSuccess, Runnable onFailure) {
    if (isFineLocationPermissionGranted()) {
      onSuccess.run();
      return;
    }
    requestPermissions(FINE_LOCATION_PERMISSIONS, onSuccess, onFailure);
  }

  private void requestPermissions(String[] permissions, Runnable onSuccess, Runnable onFailure) {
    callbacks.put(FINE_LOCATION_PERMISSIONS_REQUEST_CODE, new Pair(onSuccess, onFailure));
    ActivityCompat.requestPermissions(
        activity, permissions, FINE_LOCATION_PERMISSIONS_REQUEST_CODE);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Pair<Runnable, Runnable> callbackPair = callbacks.get(requestCode);
    if (callbackPair == null || requestCode != FINE_LOCATION_PERMISSIONS_REQUEST_CODE) {
      return;
    }
    Runnable onSuccess = callbackPair.first;
    Runnable onFailure = callbackPair.second;
    for (int grantResult : grantResults) {
      if (grantResult == PackageManager.PERMISSION_GRANTED) {
        onSuccess.run();
        return;
      }
    }
    onFailure.run();
  }
}
