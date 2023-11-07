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
package com.google.android.ground.ui.home.mapcontainer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.ground.ui.home.mapcontainer.cards.LoiCardUtil.getDisplayLoiName
import com.google.android.ground.ui.home.mapcontainer.cards.LoiCardUtil.getJobName
import com.google.android.ground.ui.home.mapcontainer.cards.LoiCardUtil.getSubmissionsText
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoiCardUtilTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun testLoiNameWithPoint_whenCustomIdAndPropertiesAreNull() {
    assertThat(getDisplayLoiName(context, TEST_LOI.copy(customId = "", properties = mapOf())))
      .isEqualTo("Unnamed point")
  }

  @Test
  fun testLoiNameWithPolygon_whenCustomIdAndPropertiesAreNull() {
    assertThat(getDisplayLoiName(context, TEST_AREA.copy(customId = "", properties = mapOf())))
      .isEqualTo("Unnamed area")
  }

  @Test
  fun testLoiName_whenCustomIdIsAvailable() {
    assertThat(getDisplayLoiName(context, TEST_LOI.copy(customId = "some value")))
      .isEqualTo("Point (some value)")
  }

  @Test
  fun testArea_whenCustomIdIsAvailable() {
    assertThat(getDisplayLoiName(context, TEST_AREA.copy(customId = "some value")))
      .isEqualTo("Area (some value)")
  }

  @Test
  fun testArea_whenCustomIdIsNotAvailable_usesPropertiesId() {
    assertThat(
        getDisplayLoiName(
          context,
          TEST_AREA.copy(customId = "", properties = mapOf("id" to "property id"))
        )
      )
      .isEqualTo("Area (property id)")
  }

  @Test
  fun testLoiName_whenPropertiesNameIsAvailable() {
    assertThat(
        getDisplayLoiName(context, TEST_LOI.copy(properties = mapOf("name" to "custom name")))
      )
      .isEqualTo("custom name")
  }

  @Test
  fun testLoiName_whenCustomIdAndPropertiesNameIsAvailable() {
    assertThat(
        getDisplayLoiName(
          context,
          TEST_LOI.copy(customId = "some value", properties = mapOf("name" to "custom name"))
        )
      )
      .isEqualTo("custom name (some value)")
  }

  @Test
  fun testLoiName_whenPropertiesDoesNotContainName() {
    assertThat(
        getDisplayLoiName(
          context,
          TEST_LOI.copy(customId = "", properties = mapOf("not" to "a name field"))
        )
      )
      .isEqualTo("Unnamed point")
  }

  @Test
  fun testLoiJobName_whenNameIsNull() {
    val job = TEST_LOI.job.copy(name = null)
    assertThat(getJobName(TEST_LOI.copy(job = job))).isNull()
  }

  @Test
  fun testLoiJobName_whenNameIsAvailable() {
    val job = TEST_LOI.job.copy(name = "job name")
    assertThat(getJobName(TEST_LOI.copy(job = job))).isEqualTo("job name")
  }

  @Test
  fun testSubmissionsText_whenZero() {
    assertThat(getSubmissionsText(0)).isEqualTo("No submissions")
  }

  @Test
  fun testSubmissionsText_whenOne() {
    assertThat(getSubmissionsText(1)).isEqualTo("1 submission")
  }

  @Test
  fun testSubmissionsText_whenTwo() {
    assertThat(getSubmissionsText(2)).isEqualTo("2 submissions")
  }

  companion object {
    private val TEST_LOI = FakeData.LOCATION_OF_INTEREST.copy()
    private val TEST_AREA = FakeData.AREA_OF_INTEREST.copy()
  }
}
