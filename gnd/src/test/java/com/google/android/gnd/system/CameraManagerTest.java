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
import android.content.Intent;
import android.graphics.Bitmap;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.rx.SchedulersModule;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import java.util.NoSuchElementException;
import java8.util.function.Consumer;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@SuppressWarnings("unchecked")
@HiltAndroidTest
@UninstallModules({SchedulersModule.class, LocalDatabaseModule.class})
@Config(application = HiltTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class CameraManagerTest {

  private static final int REQUEST_CODE = CameraManager.CAPTURE_PHOTO_REQUEST_CODE;

  @Rule public MockitoRule rule = MockitoJUnit.rule();

  @Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Mock PermissionsManager mockPermissionsManager;
  @Inject ActivityStreams activityStreams;

  private CameraManager cameraManager;

  @Before
  public void setUp() {
    hiltRule.inject();
    cameraManager = new CameraManager(mockPermissionsManager, activityStreams, null);
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
    cameraManager.capturePhoto().test().assertNoErrors();

    requests.assertValueCount(1);
  }

  @Test
  public void testLaunchPhotoCapture_whenPermissionDenied() {
    TestObserver<Consumer<Activity>> requests = activityStreams.getActivityRequests().test();

    mockPermissions(false);
    cameraManager.capturePhoto().test().assertFailure(PermissionDeniedException.class);

    requests.assertNoValues();
  }

  @Test
  public void testCapturePhotoResult_nullIntent() {
    TestObserver<Bitmap> subscriber = cameraManager.capturePhotoResult().test();
    activityStreams.onActivityResult(REQUEST_CODE, Activity.RESULT_OK, null);
    subscriber.assertFailure(NoSuchElementException.class);
  }

  @Test
  public void testCapturePhotoResult_emptyExtras() {
    TestObserver<Bitmap> subscriber = cameraManager.capturePhotoResult().test();
    activityStreams.onActivityResult(REQUEST_CODE, Activity.RESULT_OK, new Intent());
    subscriber.assertFailure(NoSuchElementException.class);
  }

  @Test
  public void testCapturePhotoResult_requestCancelled() {
    TestObserver<Bitmap> subscriber = cameraManager.capturePhotoResult().test();
    activityStreams.onActivityResult(REQUEST_CODE, Activity.RESULT_CANCELED, null);
    subscriber.assertResult();
  }

  @Test
  public void testPhotoPickerResult() throws IOException {
    Bitmap mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);

    TestObserver<Bitmap> subscriber = cameraManager.capturePhotoResult().test();

    activityStreams.onActivityResult(
        REQUEST_CODE, Activity.RESULT_OK, new Intent().putExtra("data", mockBitmap));

    subscriber.assertResult(mockBitmap);
  }
}
