/*
 * Copyright 2021 Google LLC
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

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import java.util.Map;
import java8.util.stream.Collectors;
import java8.util.stream.IntStreams;

/** Represents the arguments of an {@link Activity#onActivityResult(int, int, Intent)} event. */
public class RequestPermissionsResult {

  private final int requestCode;
  private final Map<String, Integer> permissionGrantResults;

  RequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    this.requestCode = requestCode;
    this.permissionGrantResults =
        IntStreams.range(0, permissions.length)
            .boxed()
            .collect(Collectors.toMap(i -> permissions[i], i -> grantResults[i]));
  }

  /** Returns the permissions request code for which this result applies. */
  public int getRequestCode() {
    return requestCode;
  }

  /**
   * Returns true iff the event indicated the specified permission is granted to the application.
   */
  public boolean isGranted(String permission) {
    Integer grantResult = permissionGrantResults.get(permission);
    return grantResult != null && grantResult == PERMISSION_GRANTED;
  }
}
