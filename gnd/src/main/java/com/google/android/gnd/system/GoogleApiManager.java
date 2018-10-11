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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gnd.inject.ActivityScoped;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import javax.inject.Inject;

@ActivityScoped
public class GoogleApiManager {
  private static final int INSTALL_API_REQUEST_CODE =
      GoogleApiAvailability.class.hashCode() & 0xffff;
  private final GoogleApiAvailability googleApiAvailability;
  // TODO: Replace with CompletableSubject?
  @Nullable private CompletableEmitter installApiResultEmitter;
  private int googleApiAvailabilityResult;

  @Inject
  public GoogleApiManager() {
    this.googleApiAvailability = GoogleApiAvailability.getInstance();
    this.googleApiAvailabilityResult = ConnectionResult.UNKNOWN;
  }

  public Completable installGooglePlayServices(FragmentActivity activity) {
    return Completable.create(
        src -> {
          installApiResultEmitter = src;
          googleApiAvailabilityResult =
              googleApiAvailability.isGooglePlayServicesAvailable(activity);
          if (googleApiAvailabilityResult == ConnectionResult.SUCCESS) {
            src.onComplete();
          } else {
            googleApiAvailability.showErrorDialogFragment(
                activity,
                googleApiAvailabilityResult,
                INSTALL_API_REQUEST_CODE,
                di -> src.onError(cancelled()));
          }
        });
  }

  @NonNull
  private RuntimeException cancelled() {
    return new RuntimeException("Google Play Services install cancelled");
  }

  public void onActivityResult(int requestCode, int resultCode) {
    if (requestCode == INSTALL_API_REQUEST_CODE && installApiResultEmitter != null) {
      switch (resultCode) {
        case Activity.RESULT_OK:
          installApiResultEmitter.onComplete();
          break;
        case Activity.RESULT_CANCELED:
          installApiResultEmitter.onError(cancelled());
          break;
        default:
          break;
      }
    }
  }
}
