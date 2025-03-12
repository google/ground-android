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
package org.groundplatform.android.system

import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.groundplatform.android.R
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.ui.map.Bounds
import org.groundplatform.android.ui.map.gms.GmsExt.center
import timber.log.Timber

/** Abstracts native geocoding facilities. */
@Singleton
class GeocodingManager
@Inject
constructor(
  private val geocoder: Geocoder,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  resources: Resources,
) {
  private val defaultAreaName: String = resources.getString(R.string.unnamed_area)
  private val multipleRegionsLabel: String = resources.getString(R.string.multiple_regions)

  /**
   * Retrieve a human readable name for the region bounded by the provided {@param bounds}.
   *
   * If no area name is found for the given area, returns a default value.
   */
  @Throws(IOException::class)
  suspend fun getAreaName(bounds: Bounds): String {
    // Get potential addresses of five sample points: the centroid and the four vertices of the
    // bounding box.
    val samplePoints = bounds.corners + bounds.center()
    val samplePointAddresses =
      withContext(ioDispatcher) { samplePoints.map { fetchAddressesBlocking(it) } }
    val nameComponents =
      findCommonComponents(
        samplePointAddresses.filter { it.isNotEmpty() },
        Address::getCountryName,
        Address::getAdminArea,
        Address::getSubAdminArea,
        Address::getLocality,
      )
    return when (nameComponents.size) {
      0 -> defaultAreaName
      1 -> "$multipleRegionsLabel, ${nameComponents.first()}"
      else -> nameComponents.joinToString(", ")
    }
  }

  /**
   * Calls getters in order on all [Address]es and returns names present in all other entries
   * present in [samplePointAddresses]. If a common name is not found, searching stops and further
   * getters are not called. This allows the caller to build an area name out of multiple addresses
   * by finding the largest common admin unit name among provided addresses.
   */
  private fun findCommonComponents(
    samplePointAddresses: List<List<Address>>,
    vararg getters: (Address) -> String?,
  ): MutableList<String> {
    val commonComponents = mutableListOf<String>()
    for (getter in getters) {
      val samplePointNames =
        samplePointAddresses.map { it.mapNotNull(getter::invoke) }.filter { it.isNotEmpty() }
      val commonElements = getCommonComponents(samplePointNames)
      if (commonElements.isNotEmpty()) {
        // Choose the shortest common name. If two names have the same length, use whichever comes
        // first alphabetically.
        val selectedName = commonElements.sortedWith(compareBy({ it.length }, { it })).first()
        commonComponents.add(0, selectedName)
      }
    }
    return commonComponents
  }

  /**
   * Returns the distinct list of Strings which appear at least once in all sub-lists, or an empty
   * list if none are found.
   */
  private fun getCommonComponents(samplePointNames: List<List<String>>): List<String> =
    // Require at least two vertices with non-null area labels to identify a common name.
    if (samplePointNames.size < 2) listOf()
    else
      samplePointNames.first().distinct().filter { el ->
        samplePointNames.drop(0).all { it.contains(el) }
      }

  /**
   * Fetches potential addresses for the specified coordinates. Blocks the thread on I/O; do not
   * call on main thread.
   */
  private fun fetchAddressesBlocking(coordinates: Coordinates): List<Address> =
    // TODO: Replace with non-blocking call with listener.
    // Issue URL: https://github.com/google/ground-android/issues/1762
    try {
      geocoder.getFromLocation(coordinates.lat, coordinates.lng, 5) ?: listOf()
    } catch (e: Exception) {
      Timber.e(e, "Reverse geocode lookup failed")
      listOf()
    }
}
