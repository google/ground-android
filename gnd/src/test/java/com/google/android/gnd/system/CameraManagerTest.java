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

import android.Manifest.permission;
import android.app.Activity;
import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.observers.TestObserver;
import java.io.File;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class CameraManagerTest extends BaseHiltTest {

  private static final int REQUEST_CODE = CameraManager.CAPTURE_PHOTO_REQUEST_CODE;
  private static final File TEST_FILE = new File("foo/dir");

  @Inject ActivityStreams activityStreams;
  @Inject CameraManager cameraManager;
  @Inject TestPermissionUtil permissionUtil;

  private void mockPermissions(boolean allow) {
    String[] permissions = {permission.WRITE_EXTERNAL_STORAGE, permission.CAMERA};
    permissionUtil.setPermission(permissions, allow);
  }

  @Test
  public void testLaunchPhotoCapture_whenPermissionGranted() {
    TestObserver<Nil> testObserver = cameraManager.capturePhoto(TEST_FILE).test();
    mockPermissions(true);
    testObserver.assertNoErrors();
  }

  @Test
  public void testLaunchPhotoCapture_whenPermissionDenied() {
    TestObserver<Nil> testObserver = cameraManager.capturePhoto(TEST_FILE).test();
    mockPermissions(false);
    testObserver.assertError(PermissionDeniedException.class);
  }

  @Test
  public void testCapturePhotoResult_requestCancelled() {
    TestObserver<Nil> subscriber = cameraManager.capturePhotoResult().test();
    activityStreams.onActivityResult(REQUEST_CODE, Activity.RESULT_CANCELED, null);
    subscriber.assertResult();
  }
}
