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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gnd.rx.RxLocationServices;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

@Singleton
public class SettingsManager {
  private static final String TAG = SettingsManager.class.getSimpleName();
  private static final int CHECK_SETTINGS_REQUEST_CODE = SettingsManager.class.hashCode() & 0xffff;

  private final Context context;
  private final Subject<SettingsChangeRequest> settingsChangeRequestSubject;
  private CompletableEmitter settingsChangeResultEmitter;

  @Inject
  public SettingsManager(Application app) {
    this.context = app.getApplicationContext();
    this.settingsChangeRequestSubject = PublishSubject.create();
  }

  public Observable<SettingsChangeRequest> settingsChangeRequests() {
    return settingsChangeRequestSubject;
  }

  public Completable enableLocationSettings(LocationRequest locationRequest) {
    Log.d(TAG, "Checking location settings");
    LocationSettingsRequest settingsRequest =
        new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
    return RxLocationServices.getSettingsClient(context)
        .checkLocationSettings(settingsRequest)
        .toCompletable()
        .doOnComplete(() -> Log.d(TAG, "Location settings already enabled"))
        .onErrorResumeNext(this::onCheckLocationSettingsFailure);
  }

  private Completable onCheckLocationSettingsFailure(Throwable t) {
    if ((t instanceof ResolvableApiException) && isResolutionRequired((ResolvableApiException) t)) {
      Log.d(TAG, "Prompting user to enable location settings");
      // Attach settings change result stream to Completable returned by checkLocationSettings().
      Completable completable = Completable.create(src -> this.settingsChangeResultEmitter = src);
      // Prompt user to enable Location in Settings.
      settingsChangeRequestSubject.onNext(new SettingsChangeRequest((ResolvableApiException) t));
      return completable;
    } else {
      Log.d(TAG, "Unable to prompt user to enable location settings");
      return Completable.error(t);
    }
  }

  private boolean isResolutionRequired(ResolvableApiException e) {
    return e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED;
  }

  public void onActivityResult(int requestCode, int resultCode) {
    if (requestCode != CHECK_SETTINGS_REQUEST_CODE || settingsChangeResultEmitter == null) {
      return;
    }
    Log.d(TAG, "Location settings resultCode received: " + resultCode);
    switch (resultCode) {
      case Activity.RESULT_OK:
        settingsChangeResultEmitter.onComplete();
        break;
      case Activity.RESULT_CANCELED:
        settingsChangeResultEmitter.onError(new SettingsChangeRequestCanceled());
        break;
      default:
        break;
    }
  }

  public static class SettingsChangeRequest {
    private ResolvableApiException exception;
    private int requestCode;

    private SettingsChangeRequest(ResolvableApiException exception) {
      this.exception = exception;
      this.requestCode = CHECK_SETTINGS_REQUEST_CODE;
    }

    public ResolvableApiException getException() {
      return exception;
    }

    public int getRequestCode() {
      return requestCode;
    }
  }

  public static class SettingsChangeRequestCanceled extends Exception {}
}
