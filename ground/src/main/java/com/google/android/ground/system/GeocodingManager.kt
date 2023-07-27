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
import android.location.Address
import android.location.Geocoder
import com.google.android.ground.R
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.GmsExt.center
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

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
  private val multipleRegionsLabel: String = resources.getString(R.string.multiple_regions)

  /**
   * Retrieve a human readable name for the region bounded by the provided {@param bounds}.
   *
   * If no area name is found for the given area, returns a default value.
   */
  @Throws(IOException::class)
  suspend fun getAreaName(bounds: Bounds, sensitivity: Double): String {
    // Get potential addresses of center and four corners between viewport and center.
    val vertices = bounds.shrink(sensitivity).corners + bounds.center()
    val vertexAddresses = vertices.map { fetchAddresses(it) }.filter { it.isNotEmpty() }
    val nameComponents =
      findCommonComponents(
        vertexAddresses,
        Address::getCountryName,
        Address::getAdminArea,
        Address::getSubAdminArea,
        Address::getLocality
      )
    if (nameComponents.size == 1) {
      nameComponents.add(0, multipleRegionsLabel)
    }
    return nameComponents.joinToString(", ").ifEmpty { defaultAreaName }
  }

  private fun findCommonComponents(
    addressesList: List<List<Address>>,
    vararg getters: (Address) -> String?
  ): MutableList<String> {
    val commonComponents = mutableListOf<String>()
    for (getter in getters) {
      val vertexNameVariants =
        addressesList.map { it.mapNotNull(getter::invoke) }.filter { it.isNotEmpty() }
      // We need at least two vertices with non-null area labels to identify a common name.
      if (vertexNameVariants.size < 2) break
      val commonElements = getCommonElements(vertexNameVariants)

      if (commonElements.isEmpty()) break
      // Choose the shortest common name. If two names have the same length, use whichever comes
      // first alphabetically.
      val selectedName = commonElements.sortedWith(compareBy({ it.length }, { it })).first()
      commonComponents.add(0, selectedName)
    }
    return commonComponents
  }

  /**
   * Returns the distinct list of Strings which appear at least once in all sub-lists, or an empty
   * list if none are found.
   */
  private fun getCommonElements(lists: List<List<String>>): List<String> =
    lists.firstOrNull()?.distinct()?.filter { el -> lists.drop(0).all { it.contains(el) } }
      ?: listOf()

  /** Fetches potential addresses at the specified coordinates. */
  private suspend fun fetchAddresses(coordinate: Coordinate): List<Address> =
    withContext(ioDispatcher) {
      // TODO(#1762): Replace with non-blocking call with listener.
      try {
        geocoder.getFromLocation(coordinate.lat, coordinate.lng, 5) ?: listOf()
      } catch (e: Exception) {
        Timber.e(e, "Reverse geocode lookup failed")
        return@withContext listOf()
      }
    }
}
