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
import android.app.Application;
import android.content.Context;
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
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
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
  private final Context context;
  private final PermissionsManager permissionsManager;
  private final SettingsManager settingsManager;
  private LocationCallbackImpl locationCallback;

  @Inject
  public LocationManager(Application app, PermissionsManager permissionsManager,
      SettingsManager settingsManager) {
    this.context = app.getApplicationContext();
    this.permissionsManager = permissionsManager;
    this.settingsManager = settingsManager;
  }

  private Completable enableLocationSettings() {
    return settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST);
  }

  private static Point toPoint(Location location) {
    return Point.newBuilder()
        .setLatitude(location.getLatitude())
        .setLongitude(location.getLongitude())
        .build();
  }

  public Observable<Point> enableLocationUpdates() {
    return permissionsManager
        .obtainFineLocationPermission()
        .andThen(enableLocationSettings())
        .andThen(getLocationUpdates());
  }

  private Observable<Point> getLocationUpdates() {
    return Observable.create(source -> {
      lastLocation().subscribe(p -> {
        source.onNext(p);
        locationCallback = new LocationCallbackImpl(source);
        startFusedLocationUpdates(source);
      }, source::onError);
    });
  }

  @SuppressLint("MissingPermission")
  private void startFusedLocationUpdates(
      ObservableEmitter<Point> source) {
    Log.d(TAG, "Requesting location updates");
    getFusedLocationProviderClient(context)
        .requestLocationUpdates(FINE_LOCATION_UPDATES_REQUEST,
            locationCallback, Looper.myLooper())
        .addOnSuccessListener(__ -> {
          Log.d(TAG, "Location updates request successful");
        })
        .addOnFailureListener(source::onError);
  }

  public void removeLocationUpdates() {
    if (locationCallback != null) {
      locationCallback.onRemove();
      getFusedLocationProviderClient(context).removeLocationUpdates(locationCallback);
      locationCallback = null;
    }
  }

  @SuppressLint("MissingPermission")
  public Single<Point> lastLocation() {
    return Single.create(src -> {
      Log.d(TAG, "Requesting last known location");
      getFusedLocationProviderClient(context)
          .getLastLocation()
          .addOnSuccessListener(
              l -> {
                if (l != null) {
                  Log.d(TAG, "Got last known location");
                  src.onSuccess(toPoint(l));
                }
              })
          .addOnFailureListener(e -> src.onError(e));
    });
  }

  private static class LocationCallbackImpl extends LocationCallback {
    private final ObservableEmitter<Point> source;

    LocationCallbackImpl(ObservableEmitter<Point> source) {
      this.source = source;
    }

    @Override
    public void onLocationResult(LocationResult locationResult) {
      Location lastLocation = locationResult.getLastLocation();
      Log.v(TAG, lastLocation.toString());
      source.onNext(toPoint(lastLocation));
    }

    @Override
    public void onLocationAvailability(LocationAvailability locationAvailability) {
      if (!locationAvailability.isLocationAvailable()) {
        Log.d(TAG, "Location unavailable");
      }
    }

    void onRemove() {
      source.onComplete();
    }
  }
}
