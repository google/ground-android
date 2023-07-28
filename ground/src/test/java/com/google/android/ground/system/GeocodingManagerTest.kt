/*
 * Copyright 2023 Google LLC
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

import android.location.Address
import android.location.Geocoder
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.GmsExt.center
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.*
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.ParameterizedRobolectricTestRunner

@HiltAndroidTest
@RunWith(ParameterizedRobolectricTestRunner::class)
@UninstallModules(SystemModule::class)
@OptIn(ExperimentalCoroutinesApi::class)
class GeocodingManagerTest(
  private val expectedAreaName: String,
  private val inputs: List<Pair<Coordinate, Address>>,
) : BaseHiltTest() {
  @BindValue @Mock lateinit var geocoder: Geocoder
  @Inject lateinit var geocodingManager: GeocodingManager

  @Test
  fun `getAreaName() returns name`() = runWithTestDispatcher {
    inputs.forEach { (coordinates, address) ->
      setAddress(coordinates.lat, coordinates.lng, address)
    }

    assertEquals(expectedAreaName, geocodingManager.getAreaName(BOUNDS))
  }

  private fun setAddress(latitude: Double, longitude: Double, vararg addresses: Address) {
    whenever(geocoder.getFromLocation(eq(latitude), eq(longitude), anyInt()))
      .thenReturn(addresses.toList())
  }

  companion object {
    private const val N = 1.0
    private const val S = -1.0
    private const val E = 1.0
    private const val W = -1.0
    private val SW = Coordinate(S, W)
    private val NE = Coordinate(N, E)
    private val BOUNDS = Bounds(SW, NE)
    private val CENTER = BOUNDS.center()
    private const val LOCALITY = "Marambaia"
    private const val SUB_ADMIN_AREA = "Belém"
    private const val ADMIN_AREA = "Parà"
    private const val COUNTRY = "Brazil"
    private const val MULTIPLE_REGIONS = "Multiple regions"
    private val locality = newAddress(COUNTRY, ADMIN_AREA, SUB_ADMIN_AREA, LOCALITY)
    private val subAdminArea = newAddress(COUNTRY, ADMIN_AREA, SUB_ADMIN_AREA)
    private val adminArea = newAddress(COUNTRY, ADMIN_AREA)
    private val country = newAddress(COUNTRY)

    private fun newAddress(
      countryName: String? = null,
      adminArea: String? = null,
      subAdminArea: String? = null,
      locality: String? = null
    ) =
      mock<Address> {
        whenever(it.countryName).thenReturn(countryName)
        whenever(it.adminArea).thenReturn(adminArea)
        whenever(it.subAdminArea).thenReturn(subAdminArea)
        whenever(it.locality).thenReturn(locality)
      }

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters
    fun data() =
      listOf(
        arrayOf("$MULTIPLE_REGIONS, $COUNTRY", listOf(NE to country, SW to country)),
        arrayOf("$ADMIN_AREA, $COUNTRY", listOf(NE to adminArea, SW to adminArea))
      )
  }
}
