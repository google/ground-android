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

import static org.mockito.ArgumentMatchers.any;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import com.google.android.gnd.HiltTestWithRobolectricRunner;
import com.google.android.gnd.system.PermissionsManager.PermissionDeniedException;
import com.google.android.gnd.ui.util.BitmapUtil;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import java.util.NoSuchElementException;
import java8.util.function.Consumer;
import javax.inject.Inject;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@HiltAndroidTest
public class StorageManagerTest extends HiltTestWithRobolectricRunner {

  private static final int REQUEST_CODE = StorageManager.PICK_PHOTO_REQUEST_CODE;

  @BindValue @Mock BitmapUtil mockBitmapUtil;
  @BindValue @Mock PermissionsManager mockPermissionsManager;

  @Inject ActivityStreams activityStreams;
  @Inject StorageManager storageManager;

  private Bitmap mockBitmap() throws IOException {
    Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
    Mockito.when(mockBitmapUtil.fromUri(any(Uri.class))).thenReturn(bitmap);
    return bitmap;
  }

  private void mockPermissions(boolean allow) {
    Mockito.when(mockPermissionsManager.obtainPermission(permission.READ_EXTERNAL_STORAGE))
        .thenReturn(
            allow ? Completable.complete() : Completable.error(new PermissionDeniedException()));
  }

  @Test
  public void testLaunchPhotoPicker_whenPermissionGranted() {
    TestObserver<Consumer<Activity>> requests = activityStreams.getActivityRequests().test();

    mockPermissions(true);
    storageManager.selectPhoto().test().assertNoErrors();

    requests.assertValueCount(1);
  }

  @Test
  public void testLaunchPhotoPicker_whenPermissionDenied() {
    TestObserver<Consumer<Activity>> requests = activityStreams.getActivityRequests().test();

    mockPermissions(false);
    storageManager.selectPhoto().test().assertFailure(PermissionDeniedException.class);

    requests.assertNoValues();
  }

  @Test
  public void testPhotoPickerResult_nullIntent() {
    TestObserver<Bitmap> subscriber = storageManager.photoPickerResult().test();
    activityStreams.onActivityResult(REQUEST_CODE, Activity.RESULT_OK, null);
    subscriber.assertFailure(NoSuchElementException.class);
  }

  @Test
  public void testPhotoPickerResult_emptyData() {
    TestObserver<Bitmap> subscriber = storageManager.photoPickerResult().test();
    activityStreams.onActivityResult(REQUEST_CODE, Activity.RESULT_OK, new Intent());
    subscriber.assertFailure(NoSuchElementException.class);
  }

  @Test
  public void testPhotoPickerResult_requestCancelled() {
    TestObserver<Bitmap> subscriber = storageManager.photoPickerResult().test();
    activityStreams.onActivityResult(REQUEST_CODE, Activity.RESULT_CANCELED, null);
    subscriber.assertResult();
  }

  @Test
  public void testPhotoPickerResult() throws IOException {
    Bitmap mockBitmap = mockBitmap();

    TestObserver<Bitmap> subscriber = storageManager.photoPickerResult().test();

    activityStreams.onActivityResult(
        REQUEST_CODE, Activity.RESULT_OK, new Intent().setData(Uri.parse("foo")));

    subscriber.assertResult(mockBitmap);
  }
}
