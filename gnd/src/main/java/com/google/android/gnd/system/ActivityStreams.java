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

/** Bridge between the {@link Activity} and various {@code Manager} classes. */
@Singleton
public class ActivityStreams {
  @Hot private final Subject<Consumer<Activity>> activityRequests = PublishSubject.create();
  @Hot private final Subject<ActivityResult> activityResults = PublishSubject.create();

  @Hot
  private final Subject<RequestPermissionsResult> requestPermissionsResults =
      PublishSubject.create();

  @Inject
  public ActivityStreams() {}

  /**
   * Queues the specified {@link Consumer} for execution. An instance of the current {@link
   * Activity} is provided to the {@code Consumer} when called.
   */
  public void withActivity(Consumer<Activity> callback) {
    activityRequests.onNext(callback);
  }

  /**
   * Callback used to communicate {@link Activity#onActivityResult(int, int, Intent)} events with
   * various {@code Manager} classes.
   */
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    activityResults.onNext(new ActivityResult(requestCode, resultCode, data));
  }

  /**
   * Callback used to communicate {@link Activity#onRequestPermissionsResult(int, String[], int[])}
   * events with various {@code Manager} classes.
   */
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    requestPermissionsResults.onNext(
        new RequestPermissionsResult(requestCode, permissions, grantResults));
  }

  /** Emits {@link Consumer}s to be executed in the context of the {@link Activity}. */
  @Hot
  public Observable<Consumer<Activity>> getActivityRequests() {
    return activityRequests;
  }

  /**
   * Emits calls to {@link Activity#onActivityResult(int, int, Intent)} where {@code requestCode}
   * matches the specified value.
   */
  @Hot
  public Observable<ActivityResult> getActivityResults(int requestCode) {
    return activityResults.filter(r -> r.getRequestCode() == requestCode);
  }

  /**
   * Emits the next call to {@link Activity#onActivityResult(int, int, Intent)} where {@code
   * requestCode} matches the specified value.
   */
  @Hot
  public Observable<ActivityResult> getNextActivityResult(int requestCode) {
    // TODO(#723): Define and handle timeouts.
    return getActivityResults(requestCode).take(1);
  }

  /**
   * Emits the next call to {@link Activity#onRequestPermissionsResult(int, String[], int[])} where
   * {@code requestCode} matches the specified value.
   */
  public Observable<RequestPermissionsResult> getNextRequestPermissionsResult(int requestCode) {
    // TODO(#723): Define and handle timeouts.
    return requestPermissionsResults.filter(r -> r.getRequestCode() == requestCode).take(1);
  }

  /** Represents the arguments of an {@link Activity#onActivityResult(int, int, Intent)} event. */
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
}
