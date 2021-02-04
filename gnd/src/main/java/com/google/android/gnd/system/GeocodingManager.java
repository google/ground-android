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

import static java8.util.stream.StreamSupport.stream;

import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.R;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.annotations.Cold;
import io.reactivex.Single;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java8.util.Optional;
import java8.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/** Abstracts native geocoding facilities. */
@Singleton
public class GeocodingManager {
  static class AddressNotFoundException extends Exception {
    public AddressNotFoundException(String message) {
      super(message);
    }
  }

  private final Geocoder geocoder;
  private final Schedulers schedulers;
  private final String defaultAreaName;

  @Inject
  public GeocodingManager(Geocoder geocoder, Schedulers schedulers, Resources resources) {
    this.geocoder = geocoder;
    this.schedulers = schedulers;
    this.defaultAreaName = resources.getString(R.string.unnamed_area);
  }

  /**
   * Retrieve a human readable name for the region bounded by the provided {@param bounds}.
   *
   * <p>If no area name is found for the given area, returns a default value.
   */
  @Cold
  public Single<String> getAreaName(LatLngBounds bounds) {
    return Single.fromCallable(() -> getAreaNameInternal(bounds))
        .doOnError(throwable -> Timber.e(throwable, "Couldn't get address for bounds: %s", bounds))
        .subscribeOn(schedulers.io());
  }

  private String getAreaNameInternal(LatLngBounds bounds)
      throws AddressNotFoundException, IOException {
    LatLng center = bounds.getCenter();

    List<Address> addresses = geocoder.getFromLocation(center.latitude, center.longitude, 1);
    if (addresses.isEmpty()) {
      throw new AddressNotFoundException("No address found for area.");
    }

    Address address = addresses.get(0);

    // TODO(#613): Decide exactly what set of address parts we want to show the user.
    String country = Optional.ofNullable(address.getCountryName()).orElse("");
    String locality = Optional.ofNullable(address.getLocality()).orElse("");
    String admin = Optional.ofNullable(address.getAdminArea()).orElse("");
    String subAdmin = Optional.ofNullable(address.getSubAdminArea()).orElse("");
    Collection<String> components =
        new ArrayList<>(Arrays.asList(country, locality, admin, subAdmin));

    String fullLocationName =
        stream(components).filter(x -> !x.isEmpty()).collect(Collectors.joining(", "));

    return fullLocationName.isEmpty() ? defaultAreaName : fullLocationName;
  }
}
