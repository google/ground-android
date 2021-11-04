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

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.google.android.gnd.system.PermissionsManager.PERMISSIONS_REQUEST_CODE;

import android.app.Application;
import javax.inject.Inject;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

/** Test utility class to mock system permissions using Robolectric shadows. */
public class TestPermissionUtil {

  @Inject Application application;
  @Inject ActivityStreams activityStreams;

  @Inject
  TestPermissionUtil() {}

  public void setPermission(String[] permissions, boolean isGranted) {
    ShadowApplication app = Shadows.shadowOf(application);

    if (isGranted) {
      app.grantPermissions(permissions);
    } else {
      app.denyPermissions(permissions);

      // PermissionManager waits for user input if the permission is not already granted before
      // returning success/failure.
      activityStreams.onRequestPermissionsResult(
          PERMISSIONS_REQUEST_CODE, new String[] {permissions[0]}, new int[] {PERMISSION_DENIED});
    }
  }
}
