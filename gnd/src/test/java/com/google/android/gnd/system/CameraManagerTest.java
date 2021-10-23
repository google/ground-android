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
import com.google.android.gnd.HiltTestWithRobolectricRunner;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import java.io.File;
import java8.util.function.Consumer;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@HiltAndroidTest
public class CameraManagerTest extends HiltTestWithRobolectricRunner {

  private static final int REQUEST_CODE = CameraManager.CAPTURE_PHOTO_REQUEST_CODE;

  @Mock PermissionsManager mockPermissionsManager;
  @Inject ActivityStreams activityStreams;

  private CameraManager cameraManager;
  private File testFile;

  @Before
  public void setUp() {
    super.setUp();
    cameraManager = new CameraManager(null, mockPermissionsManager, activityStreams);
    testFile = new File("foo_path");
  }

  private void mockPermissions(boolean allow) {
    String[] permissions = {permission.WRITE_EXTERNAL_STORAGE, permission.CAMERA};
    for (String permission : permissions) {
      Mockito.when(mockPermissionsManager.obtainPermission(permission))
          .thenReturn(
              allow ? Completable.complete() : Completable.error(new PermissionDeniedException()));
    }
  }

  @Test
  public void testLaunchPhotoCapture_whenPermissionGranted() {
    TestObserver<Consumer<Activity>> requests = activityStreams.getActivityRequests().test();

    mockPermissions(true);
    cameraManager.capturePhoto(testFile).test().assertNoErrors();

    requests.assertValueCount(1);
  }

  @Test
  public void testLaunchPhotoCapture_whenPermissionDenied() {
    TestObserver<Consumer<Activity>> requests = activityStreams.getActivityRequests().test();

    mockPermissions(false);
    cameraManager.capturePhoto(testFile).test().assertFailure(PermissionDeniedException.class);

    requests.assertNoValues();
  }

  @Test
  public void testCapturePhotoResult_requestCancelled() {
    TestObserver<Nil> subscriber = cameraManager.capturePhotoResult().test();
    activityStreams.onActivityResult(REQUEST_CODE, Activity.RESULT_CANCELED, null);
    subscriber.assertResult();
  }
}
