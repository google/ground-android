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

import android.app.Application;
import android.content.Context;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gnd.inject.ActivityScoped;
import io.reactivex.Completable;
import javax.inject.Inject;

@ActivityScoped
public class GoogleApiManager {
  private static final int INSTALL_API_REQUEST_CODE =
      GoogleApiAvailability.class.hashCode() & 0xffff;
  private final GoogleApiAvailability googleApiAvailability;
  private final Context context;
  private final ActivityStreams activityStreams;

  @Inject
  public GoogleApiManager(
      Application app,
      GoogleApiAvailability googleApiAvailability,
      ActivityStreams activityStreams) {
    this.context = app.getApplicationContext();
    this.googleApiAvailability = googleApiAvailability;
    this.activityStreams = activityStreams;
  }

  /**
   * Returns a stream that either completes immediately if Google Play Services are already
   * installed, otherwise shows install dialog. Terminates with error if install not possible or
   * cancelled.
   */
  public Completable installGooglePlayServices() {
    return requestInstallOrComplete().ambWith(getNextInstallApiResult());
  }

  private Completable requestInstallOrComplete() {
    return Completable.create(
        em -> {
          int status = googleApiAvailability.isGooglePlayServicesAvailable(context);
          if (status == ConnectionResult.SUCCESS) {
            em.onComplete();
          } else if (googleApiAvailability.isUserResolvableError(status)) {
            // TODO: Throw appropriate Exception.
            activityStreams.withActivity(
                activity ->
                    googleApiAvailability.showErrorDialogFragment(
                        activity,
                        status,
                        INSTALL_API_REQUEST_CODE,
                        dialogInterface -> em.onError(new Exception())));

          } else {
            // TODO: Throw appropriate Exception.
            em.onError(new Exception());
          }
        });
  }

  private Completable getNextInstallApiResult() {
    // TODO: Throw appropriate Exception.
    return activityStreams
        .getResults()
        .filter(r -> r.getRequestCode() == INSTALL_API_REQUEST_CODE)
        .take(1)
        .flatMapCompletable(r -> r.toCompletableOrError(() -> new Exception()));
  }
}
