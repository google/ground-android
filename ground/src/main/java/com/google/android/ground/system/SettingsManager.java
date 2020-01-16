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

package com.google.android.ground.system;

import static com.google.android.ground.rx.RxCompletable.completeOrError;

import android.app.Application;
import android.content.Context;
import android.content.IntentSender.SendIntentException;
import android.util.Log;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.ground.rx.RxLocationServices;
import io.reactivex.Completable;
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
  private static final int LOCATION_SETTINGS_REQUEST_CODE =
      SettingsManager.class.hashCode() & 0xffff;

  private final Context context;
  private final ActivityStreams activityStreams;

  @Inject
  public SettingsManager(Application app, ActivityStreams activityStreams) {
    this.context = app.getApplicationContext();
    this.activityStreams = activityStreams;
  }

  /**
   * Try to enable location settings. If location settings are already enabled, this will complete
   * immediately on subscribe.
   */
  public Completable enableLocationSettings(LocationRequest locationRequest) {
    Log.d(TAG, "Checking location settings");
    LocationSettingsRequest settingsRequest =
        new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
    return RxLocationServices.getSettingsClient(context)
        .checkLocationSettings(settingsRequest)
        .toCompletable()
        .onErrorResumeNext(t -> onCheckSettingsFailure(LOCATION_SETTINGS_REQUEST_CODE, t));
  }

  private Completable onCheckSettingsFailure(int requestCode, Throwable t) {
    if (!(t instanceof ResolvableApiException)) {
      return Completable.error(t);
    }
    return startResolution(requestCode, (ResolvableApiException) t)
        .andThen(getNextResult(requestCode));
  }

  private Completable startResolution(int requestCode, ResolvableApiException resolvableException) {
    return Completable.create(
        em -> {
          Log.d(TAG, "Prompting user to enable settings");
          activityStreams.withActivity(
              act -> {
                try {
                  resolvableException.startResolutionForResult(act, requestCode);
                  em.onComplete();
                } catch (SendIntentException e) {
                  em.onError(e);
                }
              });
        });
  }

  private Completable getNextResult(int requestCode) {
    return activityStreams
        .getNextActivityResult(requestCode)
        .flatMapCompletable(r -> completeOrError(r.isOk(), SettingsChangeRequestCanceled.class))
        .doOnComplete(() -> Log.d(TAG, "Settings change request successful"))
        .doOnError(t -> Log.d(TAG, "Settings change request failed", t));
  }

  public static class SettingsChangeRequestCanceled extends Exception {}
}
