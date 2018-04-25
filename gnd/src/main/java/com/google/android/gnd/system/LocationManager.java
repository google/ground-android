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

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gnd.inject.PerActivity;
import com.google.android.gnd.model.Point;
import io.reactivex.Completable;
import java8.util.function.Consumer;
import javax.inject.Inject;

@PerActivity
public class LocationManager {
  private static final String TAG = LocationManager.class.getSimpleName();
  private static final long UPDATE_INTERVAL = 1000 /* 1 sec */;
  private static final long FASTEST_INTERVAL = 250;
  private static final LocationRequest FINE_LOCATION_UPDATES_REQUEST =
      new LocationRequest()
          .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
          .setInterval(UPDATE_INTERVAL)
          .setFastestInterval(FASTEST_INTERVAL);
  private final Activity activity;
  private final PermissionsManager permissionsManager;
  private final SettingsManager settingsManager;
  private LocationCallback locationCallback;

  @Inject
  public LocationManager(Activity context, PermissionsManager permissionsManager,
      SettingsManager settingsManager) {
    this.activity = context;
    this.permissionsManager = permissionsManager;
    this.settingsManager = settingsManager;
  }

  public Completable enableFineLocationUpdatesSettings() {
    return settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST);
  }

  private static Point toPoint(Location location) {
    return Point.newBuilder()
        .setLatitude(location.getLatitude())
        .setLongitude(location.getLongitude())
        .build();
  }

  /**
   * Must check fine-grained location permission and location settings before calling this!
   */
  @SuppressLint("MissingPermission")
  public void requestLocationUpdates(
      Runnable onSuccess,
      Consumer<LocationFailureReason> onFailure,
      Consumer<Point> onLocationUpdate) {
    LocationCallback callback = new LocationCallbackImpl(onLocationUpdate);
    getFusedLocationProviderClient(activity)
        .requestLocationUpdates(FINE_LOCATION_UPDATES_REQUEST, callback, Looper.myLooper())
        .addOnSuccessListener(
            v -> {
              locationCallback = callback;
              onSuccess.run();
            })
        .addOnFailureListener(e -> this.handleRequestLocationUpdatesFailure(e, onFailure));
  }

  private void handleRequestLocationUpdatesFailure(
      Exception e, Consumer<LocationFailureReason> onFailure) {
    Log.w(TAG, "Location updates request failed", e);
    onFailure.accept(LocationFailureReason.LOCATION_UPDATES_REQUEST_FAILED);
  }

  public void removeLocationUpdates() {
    if (locationCallback != null) {
      getFusedLocationProviderClient(activity).removeLocationUpdates(locationCallback);
      locationCallback = null;
    }
  }

  @SuppressLint("MissingPermission")
  public void requestLastLocation(Consumer<Point> onSuccess) {
    getFusedLocationProviderClient(activity)
        .getLastLocation()
        .addOnSuccessListener(
            l -> {
              if (l != null) {
                onSuccess.accept(toPoint(l));
              }
            });
  }

  public enum LocationFailureReason {
    UNEXPECTED_ERROR,
    LOCATION_UPDATES_REQUEST_FAILED,
    SETTINGS_CHANGE_FAILED,
    SETTINGS_CHANGE_UNAVAILABLE
  }

  private class LocationCallbackImpl extends LocationCallback {
    private final Consumer<Point> onLocationUpdate;

    public LocationCallbackImpl(Consumer<Point> onLocationUpdate) {
      this.onLocationUpdate = onLocationUpdate;
    }

    @Override
    public void onLocationResult(LocationResult locationResult) {
      Location lastLocation = locationResult.getLastLocation();
      onLocationUpdate.accept(toPoint(lastLocation));
    }

    @Override
    public void onLocationAvailability(LocationAvailability locationAvailability) {
      // TODO: Show warning when location no longer available.
    }
  }
}
