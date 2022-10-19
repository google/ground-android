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
import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import dagger.hilt.android.testing.HiltAndroidTest
import java8.util.Optional
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocationOfInterestHelperTest : BaseHiltTest() {

  @Inject lateinit var loiHelper: LocationOfInterestHelper

  @Test
  fun testGetCreatedBy() {
    val user = FakeData.USER.copy(displayName = TEST_USER_NAME)
    val loi = FakeData.LOCATION_OF_INTEREST.copy(created = AuditInfo(user))
    assertCreatedBy(loi, "Added by $TEST_USER_NAME")
  }

  @Test
  fun testGetCreatedBy_whenLoiIsNull() {
    assertCreatedBy(null, "")
  }

  @Test
  fun testGetLabel_whenLoiIsNull() {
    assertLabel(null, "")
  }

  @Test
  fun testGetLabel_whenCaptionIsEmptyAndLoiIsPoint() {
    val loi = FakeData.LOCATION_OF_INTEREST.copy(caption = "")
    assertLabel(loi, "Point")
  }

  @Test
  fun testGetLabel_whenCaptionIsEmptyAndLoiIsPolygon() {
    val loi = FakeData.AREA_OF_INTEREST.copy(caption = "")
    assertLabel(loi, "Polygon")
  }

  @Test
  fun testGetLabel_whenCaptionIsPresentAndLoiIsPoint() {
    val loi = FakeData.LOCATION_OF_INTEREST.copy(caption = TEST_CAPTION)
    assertLabel(loi, TEST_CAPTION)
  }

  @Test
  fun testGetLabel_whenCaptionIsPresentAndLoiIsPolygon() {
    val loi = FakeData.AREA_OF_INTEREST.copy(caption = TEST_CAPTION)
    assertLabel(loi, TEST_CAPTION)
  }

  @Test
  fun testGetSubtitle() {
    val loi = FakeData.LOCATION_OF_INTEREST.copy(job = FakeData.JOB.copy(name = TEST_JOB_NAME))
    assertSubtitle(loi, "Job: $TEST_JOB_NAME")
  }

  @Test
  fun testGetSubtitle_whenLoiIsEmpty() {
    assertSubtitle(null, "")
  }

  private fun assertCreatedBy(loi: LocationOfInterest?, expectedCreatedBy: String) {
    assertThat(loiHelper.getCreatedBy(Optional.ofNullable(loi))).isEqualTo(expectedCreatedBy)
  }

  private fun assertLabel(loi: LocationOfInterest?, expectedLabel: String) {
    assertThat(loiHelper.getLabel(Optional.ofNullable(loi))).isEqualTo(expectedLabel)
  }

  private fun assertSubtitle(loi: LocationOfInterest?, expectedSubtitle: String) {
    assertThat(loiHelper.getSubtitle(Optional.ofNullable(loi))).isEqualTo(expectedSubtitle)
  }

  companion object {
    private const val TEST_USER_NAME = "some user name"
    private const val TEST_CAPTION = "some caption text"
    private const val TEST_JOB_NAME = "some job name"
  }
}
