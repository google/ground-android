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
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.gms.GmsExt.center
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlin.test.assertEquals
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
class GeocodingManagerTest(
  private val expectedAreaName: String,
  private val message: String,
  private val inputs: List<Pair<Coordinates, List<Address>>>,
) : BaseHiltTest() {
  @BindValue @Mock lateinit var geocoder: Geocoder
  @Inject lateinit var geocodingManager: GeocodingManager

  @Test
  fun `getAreaName() returns expected name`() = runWithTestDispatcher {
    inputs.forEach { (coordinates, addresses) ->
      setAddresses(coordinates.lat, coordinates.lng, addresses)
    }

    assertEquals(expectedAreaName, geocodingManager.getAreaName(BOUNDS), message)
  }

  private fun setAddresses(latitude: Double, longitude: Double, addresses: List<Address>) {
    whenever(geocoder.getFromLocation(eq(latitude), eq(longitude), anyInt())).thenReturn(addresses)
  }

  companion object {
    private const val N = 1.0
    private const val S = -1.0
    private const val E = 1.0
    private const val W = -1.0
    private val SW = Coordinates(S, W)
    private val NE = Coordinates(N, E)
    private val NW = Coordinates(N, W)
    private val SE = Coordinates(S, E)
    private val BOUNDS = Bounds(SW, NE)
    private val CENTER = BOUNDS.center()
    private const val LOCALITY = "Marambaia"
    private const val SUB_ADMIN_AREA = "Belém"
    private const val ADMIN_AREA1 = "Parà"
    private const val ADMIN_AREA2 = "State of Parà"
    private const val COUNTRY1 = "Brazil"
    private const val COUNTRY2 = "Uruguay"
    private const val MULTIPLE_REGIONS = "Multiple regions"
    private const val UNNAMED_AREA = "Unnamed area"
    private val level1Address1 = mockAddress(COUNTRY1)
    private val level1Address2 = mockAddress(COUNTRY2)
    private val level2Address1 = mockAddress(COUNTRY1, ADMIN_AREA1)
    private val level2Address2 = mockAddress(COUNTRY1, ADMIN_AREA2)
    private val nullLevel2Address = mockAddress(COUNTRY1, null)
    private val level3Address = mockAddress(COUNTRY1, ADMIN_AREA1, SUB_ADMIN_AREA)
    private val level4Address = mockAddress(COUNTRY1, ADMIN_AREA1, SUB_ADMIN_AREA, LOCALITY)

    private fun mockAddress(
      countryName: String? = null,
      adminArea: String? = null,
      subAdminArea: String? = null,
      locality: String? = null,
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
        testCase(
          "$MULTIPLE_REGIONS, $COUNTRY1",
          "Points with matching country-only addresses",
          addresses(NE, level1Address1),
          addresses(CENTER, level1Address1),
        ),
        testCase(
          UNNAMED_AREA,
          "Points with distinct countries",
          addresses(CENTER, level1Address1),
          addresses(SW, level1Address2),
        ),
        testCase(
          "$ADMIN_AREA1, $COUNTRY1",
          "Points with matching admin areas",
          addresses(NE, level2Address1),
          addresses(NW, level2Address1),
        ),
        testCase(
          "$SUB_ADMIN_AREA, $ADMIN_AREA1, $COUNTRY1",
          "Points with matching sub-admin areas",
          addresses(NE, level3Address),
          addresses(SE, level3Address),
        ),
        testCase(
          "$LOCALITY, $SUB_ADMIN_AREA, $ADMIN_AREA1, $COUNTRY1",
          "Points with matching localities",
          addresses(NE, level4Address),
          addresses(SE, level4Address),
        ),
        testCase(
          "$ADMIN_AREA1, $COUNTRY1",
          "Points with multiple admin area names: return shortest",
          addresses(NE, level2Address2, level2Address1),
          addresses(SW, level2Address1, level2Address2),
        ),
        testCase(
          "$ADMIN_AREA1, $COUNTRY1",
          "Ignore null addresses",
          addresses(NE, level2Address2, nullLevel2Address, level2Address1),
          addresses(SE, level2Address1, level2Address2, nullLevel2Address),
        ),
        testCase(UNNAMED_AREA, "Only one point with address", addresses(NE, level2Address1)),
        testCase(UNNAMED_AREA, "All points with no address"),
      )

    private fun testCase(
      expectedAreaName: String,
      message: String,
      vararg areas: Pair<Coordinates, List<Address>>,
    ) = arrayOf(expectedAreaName, message, areas.toList())

    private fun addresses(coordinates: Coordinates, vararg addresses: Address) =
      Pair(coordinates, addresses.toList())
  }
}
