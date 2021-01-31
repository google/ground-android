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

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gnd.rx.annotations.Hot;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.Map;
import java8.util.function.Consumer;
import java8.util.stream.Collectors;
import java8.util.stream.IntStreams;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ActivityStreams {
  @Hot private final Subject<Consumer<Activity>> activityRequests = PublishSubject.create();
  @Hot private final Subject<ActivityResult> activityResults = PublishSubject.create();

  @Hot
  private final Subject<RequestPermissionsResult> requestPermissionsResults =
      PublishSubject.create();

  @Inject
  public ActivityStreams() {}

  public void withActivity(Consumer<Activity> callback) {
    activityRequests.onNext(callback);
  }

  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    activityResults.onNext(new ActivityResult(requestCode, resultCode, data));
  }

  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    requestPermissionsResults.onNext(
        new RequestPermissionsResult(requestCode, permissions, grantResults));
  }

  public Observable<Consumer<Activity>> getActivityRequests() {
    return activityRequests;
  }

  public Observable<ActivityResult> getActivityResults(int requestCode) {
    return activityResults.filter(r -> r.getRequestCode() == requestCode);
  }

  // TODO: Merge streams instead of taking one.
  public Observable<ActivityResult> getNextActivityResult(int requestCode) {
    return getActivityResults(requestCode).take(1);
  }

  public Observable<RequestPermissionsResult> getNextRequestPermissionsResult(int requestCode) {
    return requestPermissionsResults.filter(r -> r.getRequestCode() == requestCode).take(1);
  }

  public static class ActivityResult {
    private final int requestCode;
    private final int resultCode;
    @Nullable private final Intent data;

    public ActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      this.requestCode = requestCode;
      this.resultCode = resultCode;
      this.data = data;
    }

    public int getRequestCode() {
      return requestCode;
    }

    public boolean isOk() {
      return resultCode == Activity.RESULT_OK;
    }

    @Nullable
    public Intent getData() {
      return data;
    }
  }

  public static class RequestPermissionsResult {
    private final int requestCode;
    private final Map<String, Integer> permissionGrantResults;

    private RequestPermissionsResult(
        int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      this.requestCode = requestCode;
      this.permissionGrantResults =
          IntStreams.range(0, permissions.length)
              .boxed()
              .collect(Collectors.toMap(i -> permissions[i], i -> grantResults[i]));
    }

    public int getRequestCode() {
      return requestCode;
    }

    public boolean isGranted(String permission) {
      Integer grantResult = permissionGrantResults.get(permission);
      return grantResult != null && grantResult == PERMISSION_GRANTED;
    }
  }
}
