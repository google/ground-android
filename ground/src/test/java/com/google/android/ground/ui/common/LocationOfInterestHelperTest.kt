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
package com.google.android.ground.ui.common

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.locationofinterest.LOI_NAME_PROPERTY
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocationOfInterestHelperTest : BaseHiltTest() {

  @Inject lateinit var loiHelper: LocationOfInterestHelper

  @Test
  fun testGetSubtitle() {
    val loi = FakeData.LOCATION_OF_INTEREST.copy(job = FakeData.JOB.copy(name = TEST_JOB_NAME))
    assertThat(loiHelper.getSubtitle(loi)).isEqualTo("Job: $TEST_JOB_NAME")
  }

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
  fun testLoiName_whenCustomIdIsAvailable() {
    assertThat(loiHelper.getDisplayLoiName(TEST_LOI.copy(customId = "some value")))
      .isEqualTo("Point (some value)")
  }

  @Test
  fun testArea_whenCustomIdIsAvailable() {
    assertThat(loiHelper.getDisplayLoiName(TEST_AREA.copy(customId = "some value")))
      .isEqualTo("Area (some value)")
  }

  @Test
  fun testArea_whenCustomIdIsNotAvailable_usesPropertiesId() {
    assertThat(
        loiHelper.getDisplayLoiName(
          TEST_AREA.copy(customId = "", properties = mapOf("id" to "property id"))
        )
      )
      .isEqualTo("Area (property id)")
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

  companion object {
    private val TEST_LOI = FakeData.LOCATION_OF_INTEREST.copy()
    private val TEST_AREA = FakeData.AREA_OF_INTEREST.copy()
    private const val TEST_JOB_NAME = "some job name"
  }
}
