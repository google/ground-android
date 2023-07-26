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
import com.google.android.ground.R
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.GmsExt.center
import java.io.IOException
import java8.util.Optional
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Abstracts native geocoding facilities. */
@Singleton
class GeocodingManager
@Inject
constructor(
  private val geocoder: Geocoder,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  resources: Resources
) {
  private val defaultAreaName: String = resources.getString(R.string.unnamed_area)

  /**
   * Retrieve a human readable name for the region bounded by the provided {@param bounds}.
   *
   * If no area name is found for the given area, returns a default value.
   */
  @Throws(AddressNotFoundException::class, IOException::class)
  suspend fun getAreaName(bounds: Bounds): String =
    withContext(ioDispatcher) {
      val center = bounds.center()
      // TODO(#1762): Replace with non-blocking call with listener.
      val addresses = geocoder.getFromLocation(center.lat, center.lng, 1)
      if (addresses.isNullOrEmpty()) {
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
      fullLocationName.ifEmpty { defaultAreaName }
    }
}

internal class AddressNotFoundException(message: String) : Exception(message)
