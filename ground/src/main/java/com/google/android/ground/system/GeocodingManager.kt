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
package com.google.android.ground.system

import android.content.res.Resources
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.R
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Single
import java.io.IOException
import java8.util.Optional
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Abstracts native geocoding facilities. */
@Singleton
class GeocodingManager
@Inject
constructor(
  private val geocoder: Geocoder,
  private val schedulers: Schedulers,
  resources: Resources
) {
  private val defaultAreaName: String = resources.getString(R.string.unnamed_area)

  /**
   * Retrieve a human readable name for the region bounded by the provided {@param bounds}.
   *
   * If no area name is found for the given area, returns a default value.
   */
  fun getAreaName(bounds: LatLngBounds): @Cold Single<String> =
    Single.fromCallable { getAreaNameInternal(bounds) }
      .doOnError { Timber.e(it, "Couldn't get address for bounds: $bounds") }
      .subscribeOn(schedulers.io())

  @Throws(AddressNotFoundException::class, IOException::class)
  private fun getAreaNameInternal(bounds: LatLngBounds): String {
    val center = bounds.center
    val addresses = geocoder.getFromLocation(center.latitude, center.longitude, 1)
    if (addresses == null || addresses.isEmpty()) {
      throw AddressNotFoundException("No address found for area.")
    }
    val address = addresses[0]

    // TODO(#613): Decide exactly what set of address parts we want to show the user.
    val country = Optional.ofNullable(address.countryName).orElse("")
    val locality = Optional.ofNullable(address.locality).orElse("")
    val admin = Optional.ofNullable(address.adminArea).orElse("")
    val subAdmin = Optional.ofNullable(address.subAdminArea).orElse("")
    val components: Collection<String> = ArrayList(listOf(country, locality, admin, subAdmin))
    val fullLocationName = components.filter { it.isNotEmpty() }.joinToString()
    return fullLocationName.ifEmpty { defaultAreaName }
  }
}

internal class AddressNotFoundException(message: String) : Exception(message)
