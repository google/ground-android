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
import android.content.IntentSender.SendIntentException;
import android.util.Log;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gnd.rx.RxLocationServices;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages enabling of settings and related flows to/from the Activity.
 *
 * <p>Note: Currently only supports location settings, but could be expanded to support other
 * settings types in the future.
 */
@Singleton
public class SettingsManager {
  private static final String TAG = SettingsManager.class.getSimpleName();
  private static final int CHECK_SETTINGS_REQUEST_CODE = SettingsManager.class.hashCode() & 0xffff;

  private final Context context;
  private final Subject<ResolvableSettingsFailure> resolvableSettingsFailures;
  private final Subject<Integer> settingsChangeResult;

  @Inject
  public SettingsManager(Application app) {
    this.context = app.getApplicationContext();
    this.resolvableSettingsFailures = PublishSubject.create();
    this.settingsChangeResult = PublishSubject.create();
  }

  /**
   * Hot observable containing failures to enable settings that can be resolved via user
   * interaction. Past events are not replayed on subscription.
   */
  public Observable<ResolvableSettingsFailure> getResolvableSettingsFailures() {
    return resolvableSettingsFailures;
  }

  /**
   * Try to enable location settings. If location settings are already enabled, this will complete
   * immediately on subscribe. If the location settings can be enabled via user intervention, the
   * Activity will be notified via {@link #getResolvableSettingsFailures} to start the resolution
   * intent.
   */
  public Completable enableLocationSettings(LocationRequest locationRequest) {
    Log.d(TAG, "Checking location settings");
    LocationSettingsRequest settingsRequest =
        new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
    return RxLocationServices.getSettingsClient(context)
        .checkLocationSettings(settingsRequest)
        .toCompletable()
        .onErrorResumeNext(this::onCheckLocationSettingsFailure);
  }

  private Completable onCheckLocationSettingsFailure(Throwable t) {
    if (!(t instanceof ResolvableApiException)) {
      return Completable.error(t);
    }
    ResolvableApiException resolvableException = (ResolvableApiException) t;
    if (resolvableException.getStatusCode() != LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
      return Completable.error(t);
    }
    return settingsChangeResult
        .doOnSubscribe(__ -> resolve(resolvableException))
        .take(1)
        .flatMapCompletable(this::handleResult);
  }

  private void resolve(ResolvableApiException resolvableException) {
    Log.d(TAG, "Prompting user to enable location settings");
    resolvableSettingsFailures.onNext(
        new ResolvableSettingsFailure(CHECK_SETTINGS_REQUEST_CODE, resolvableException));
  }

  private Completable handleResult(int resultCode) {
    return Completable.create(
        em -> {
          switch (resultCode) {
            case Activity.RESULT_OK:
              em.onComplete();
              break;
            case Activity.RESULT_CANCELED:
              em.onError(new SettingsChangeRequestCanceled());
              break;
            default:
              em.onError(new UnknownError());
              break;
          }
        });
  }

  public void onActivityResult(int requestCode, int resultCode) {
    // TODO: Move into ActivityCallbackStreams?
    // To support multiple settings request types we'll need to pass the requestCode in
    // events as well.
    if (requestCode == CHECK_SETTINGS_REQUEST_CODE) {
      Log.d(TAG, "Location settings resultCode received: " + resultCode);
      settingsChangeResult.onNext(resultCode);
    }
  }

  public static class ResolvableSettingsFailure {
    private final int requestCode;
    private ResolvableApiException exception;

    private ResolvableSettingsFailure(int requestCode, ResolvableApiException exception) {
      this.exception = exception;
      this.requestCode = requestCode;
    }

    /** Start intent prompting user to enable settings. */
    public void showSettingsPrompt(Activity activity) {
      try {
        // The result of this call is received by activity's {@link #onActivityResult}.
        Log.d(TAG, "Sending settings resolution request");
        exception.startResolutionForResult(activity, requestCode);
      } catch (SendIntentException e) {
        // TODO: Report error to user.
        Log.e(TAG, "Error starting settings resolution intent", e);
      }
    }
  }

  public static class SettingsChangeRequestCanceled extends Exception {}
}
