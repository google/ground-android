/*
 * Copyright 2021 Google LLC
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
package org.groundplatform.android.ui.common

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.model.locationofinterest.LOI_NAME_PROPERTY
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocationOfInterestHelperTest : BaseHiltTest() {

  @Inject lateinit var loiHelper: LocationOfInterestHelper

  @Test
  fun testLoiNameWithPoint_whenCustomIdAndPropertiesAreNull() {
    assertThat(loiHelper.getDisplayLoiName(TEST_LOI.copy(customId = "", properties = mapOf())))
      .isEqualTo("Unnamed point")
  }

  @Test
  fun testLoiNameWithPolygon_whenCustomIdAndPropertiesAreNull() {
    assertThat(loiHelper.getDisplayLoiName(TEST_AREA.copy(customId = "", properties = mapOf())))
      .isEqualTo("Unnamed area")
  }

  @Test
  fun testLoiNameWithMultiPolygon_whenCustomIdAndPropertiesAreNull() {
    assertThat(
        loiHelper.getDisplayLoiName(
          TEST_LOI_WITH_MULTIPOLYGON.copy(customId = "", properties = mapOf())
        )
      )
      .isEqualTo("Unnamed area")
  }

  @Test
  fun testLoiName_whenCustomIdIsAvailable() {
    assertThat(loiHelper.getDisplayLoiName(TEST_LOI.copy(customId = "some value")))
      .isEqualTo("Unnamed point (some value)")
  }

  @Test
  fun testArea_whenCustomIdIsAvailable() {
    assertThat(loiHelper.getDisplayLoiName(TEST_AREA.copy(customId = "some value")))
      .isEqualTo("Unnamed area (some value)")
  }

  @Test
  fun testArea_whenCustomIdIsNotAvailable_usesPropertiesId() {
    assertThat(
        loiHelper.getDisplayLoiName(
          TEST_AREA.copy(customId = "", properties = mapOf("id" to "property id"))
        )
      )
      .isEqualTo("Unnamed area (property id)")
  }

  @Test
  fun testLoiName_whenPropertiesNameIsAvailable() {
    assertThat(
        loiHelper.getDisplayLoiName(
          TEST_LOI.copy(properties = mapOf(LOI_NAME_PROPERTY to "custom name"))
        )
      )
      .isEqualTo("custom name")
  }

  @Test
  fun testLoiName_whenCustomIdAndPropertiesNameIsAvailable() {
    assertThat(
        loiHelper.getDisplayLoiName(
          TEST_LOI.copy(
            customId = "some value",
            properties = mapOf(LOI_NAME_PROPERTY to "custom name"),
          )
        )
      )
      .isEqualTo("custom name (some value)")
  }

  @Test
  fun testLoiName_whenPropertiesDoesNotContainName() {
    assertThat(
        loiHelper.getDisplayLoiName(
          TEST_LOI.copy(customId = "", properties = mapOf("not" to "a name field"))
        )
      )
      .isEqualTo("Unnamed point")
  }

  @Test
  fun testLoiJobName_whenNameIsNull() {
    val job = TEST_LOI.job.copy(name = null)
    assertThat(loiHelper.getJobName(TEST_LOI.copy(job = job))).isNull()
  }

  @Test
  fun testLoiJobName_whenNameIsAvailable() {
    val job = TEST_LOI.job.copy(name = "job name")
    assertThat(loiHelper.getJobName(TEST_LOI.copy(job = job))).isEqualTo("job name")
  }

  @Test
  fun testLoiName_whenCustomIdIsNotEmptyAndGeometryIsPolygon() {
    assertThat(loiHelper.getDisplayLoiName(TEST_AREA.copy(customId = "id")))
      .isEqualTo("Unnamed area (id)")
  }

  @Test
  fun testLoiName_whenCustomIdIsNotEmptyAndGeometryIsMultiPolygon() {
    assertThat(loiHelper.getDisplayLoiName(TEST_LOI_WITH_MULTIPOLYGON.copy(customId = "id")))
      .isEqualTo("Unnamed area (id)")
  }

  @Test
  fun testLoiName_whenCustomIdIsNotEmptyAndUnsupportedGeometry() {
    try {
      loiHelper.getDisplayLoiName(TEST_LOI_WITH_LINEARRING.copy(customId = "id"))
    } catch (e: Exception) {
      assertThat(e.message)
        .isEqualTo(
          "Unsupported geometry type LinearRing(coordinates=[Coordinates(lat=0.0, lng=0.0), " +
            "Coordinates(lat=10.0, lng=10.0), Coordinates(lat=20.0, lng=20.0), Coordinates(lat=0.0, lng=0.0)])"
        )
    }
  }

  @Test
  fun testLoiName_whenCustomIdIsEmptyAndUnsupportedGeometry() {
    try {
      loiHelper.getDisplayLoiName(TEST_LOI_WITH_LINEARRING.copy(customId = ""))
    } catch (e: Exception) {
      assertThat(e.message)
        .isEqualTo(
          "Unsupported geometry type LinearRing(coordinates=[Coordinates(lat=0.0, lng=0.0), " +
            "Coordinates(lat=10.0, lng=10.0), Coordinates(lat=20.0, lng=20.0), Coordinates(lat=0.0, lng=0.0)])"
        )
    }
  }

  companion object {
    private val TEST_LOI = FakeData.LOCATION_OF_INTEREST.copy()
    private val TEST_AREA = FakeData.AREA_OF_INTEREST.copy()
    private val TEST_LOI_WITH_MULTIPOLYGON = FakeData.LOCATION_OF_INTEREST_WITH_MULTIPOLYGON.copy()
    private val TEST_LOI_WITH_LINEARRING = FakeData.LOCATION_OF_INTEREST_WITH_LINEARRING.copy()
  }
}
