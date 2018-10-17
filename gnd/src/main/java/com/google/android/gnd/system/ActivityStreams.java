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

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java8.util.function.Consumer;
import java8.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ActivityStreams {
  private final Subject<Consumer<Activity>> activityRequests;
  private final Subject<ActivityResult> activityResults;

  @Inject
  public ActivityStreams() {
    activityRequests = PublishSubject.create();
    activityResults = PublishSubject.create();
  }

  public void onCreate(AppCompatActivity activity) {
    activityRequests.as(autoDisposable(activity)).subscribe(callback -> callback.accept(activity));
  }

  public void withActivity(Consumer<Activity> callback) {
    activityRequests.onNext(callback);
  }

  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    activityResults.onNext(new ActivityResult(requestCode, resultCode, data));
  }

  public Observable<ActivityResult> getResults() {
    return activityResults;
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

    public Completable toCompletableOrError(Supplier<Throwable> t) {
      return Completable.create(
          em -> {
            switch (resultCode) {
              case Activity.RESULT_OK:
                em.onComplete();
                break;
              case Activity.RESULT_CANCELED:
                em.onError(t.get());
                break;
              default:
                break;
            }
          });
    }
  }
}
