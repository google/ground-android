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

package com.google.android.gnd.system;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.R;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.IOException;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * GeocodingManger abstracts native geocoding facilities, and provides convenience methods for using
 * geocoding functionality on Ground model objects.
 */
public class GeocodingManager {
  static class AddressNotFoundException extends Exception {
    public AddressNotFoundException(String message) {
      super(message);
    }
  }

  private final Geocoder geocoder;
  private final String defaultAreaName;

  @Inject
  public GeocodingManager(@ApplicationContext Context context) {
    this.geocoder = new Geocoder(context);
    this.defaultAreaName = context.getString(R.string.offline_areas_unknown_area);
  }

  /**
   * Performs reverse geocoding on {@param bounds} to retrieve a human readable name for the region
   * captured in the bounds.
   *
   * <p>If no address is found for the given area, returns a default value.
   */
  public String getOfflineAreaName(LatLngBounds bounds) {
    LatLng center = bounds.getCenter();

    try {
      List<Address> addresses = geocoder.getFromLocation(center.latitude, center.longitude, 1);

      if (addresses.isEmpty()) {
        throw new AddressNotFoundException("No address found for area.");
      }

      Address address = addresses.get(0);

      return Optional.ofNullable(address.getFeatureName())
          .or(() -> Optional.ofNullable(address.getLocality()))
          .or(() -> Optional.ofNullable(address.getSubAdminArea()))
          .or(() -> Optional.ofNullable(address.getAdminArea()))
          .or(() -> Optional.ofNullable(address.getCountryName()))
          .orElse(defaultAreaName);

    } catch (AddressNotFoundException | IOException e) {
      Timber.e(e, "Couldn't get address for lat: %f, lng: %f", center.latitude, center.longitude);
    }

    return defaultAreaName;
  }
}
