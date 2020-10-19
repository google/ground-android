/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.system.rx;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gnd.rx.RxTask;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Single;
import javax.inject.Inject;

/** Thin wrapper around {@link SettingsClient} exposing key features as reactive streams. */
public class RxSettingsClient {

  @NonNull
  private final SettingsClient settingsClient;

  @Inject
  public RxSettingsClient(@NonNull @ApplicationContext Context context) {
    this.settingsClient = LocationServices.getSettingsClient(context);
  }

  @NonNull
  public Single<LocationSettingsResponse> checkLocationSettings(
      LocationSettingsRequest settingsRequest) {
    return RxTask.toSingle(() -> settingsClient.checkLocationSettings(settingsRequest));
  }
}
