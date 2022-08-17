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

package com.google.android.ground.ui.common;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.Nullable;
import com.google.android.ground.BaseHiltTest;
import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.test.FakeData;
import dagger.hilt.android.testing.HiltAndroidTest;
import java8.util.Optional;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class LocationOfInterestHelperTest extends BaseHiltTest {

  @Inject LocationOfInterestHelper loiHelper;

  @Test
  public void testGetCreatedBy() {
    LocationOfInterest loi =
        new LocationOfInterest(
            FakeData.POINT_OF_INTEREST.getId(),
            FakeData.POINT_OF_INTEREST.getSurvey(),
            FakeData.POINT_OF_INTEREST.getJob(),
            FakeData.POINT_OF_INTEREST.getCustomId(),
            FakeData.POINT_OF_INTEREST.getCaption(),
            AuditInfo.now(FakeData.USER.toBuilder().setDisplayName("Test User").build()),
            FakeData.POINT_OF_INTEREST.getLastModified(),
            FakeData.POINT_OF_INTEREST.getGeometry());
    assertCreatedBy(loi, "Added by Test User");
  }

  @Test
  public void testGetCreatedBy_whenLoiIsEmpty() {
    assertCreatedBy(null, "");
  }

  @Test
  public void testGetLabel_whenLoiIsEmpty() {
    assertLabel(null, "");
  }

  @Test
  public void testGetLabel_whenCaptionIsEmptyAndLoiIsPoint() {
    LocationOfInterest loi =
        new LocationOfInterest(
            FakeData.POINT_OF_INTEREST.getId(),
            FakeData.POINT_OF_INTEREST.getSurvey(),
            FakeData.POINT_OF_INTEREST.getJob(),
            FakeData.POINT_OF_INTEREST.getCustomId(),
            "",
            FakeData.POINT_OF_INTEREST.getCreated(),
            FakeData.POINT_OF_INTEREST.getLastModified(),
            FakeData.POINT_OF_INTEREST.getGeometry());
    assertLabel(loi, "Point");
  }

  @Test
  public void testGetLabel_whenCaptionIsEmptyAndLoiIsPolygon() {
    LocationOfInterest loi =
        new LocationOfInterest(
            FakeData.AREA_OF_INTEREST.getId(),
            FakeData.AREA_OF_INTEREST.getSurvey(),
            FakeData.AREA_OF_INTEREST.getJob(),
            FakeData.AREA_OF_INTEREST.getCustomId(),
            "",
            FakeData.AREA_OF_INTEREST.getCreated(),
            FakeData.AREA_OF_INTEREST.getLastModified(),
            FakeData.AREA_OF_INTEREST.getGeometry());
    assertLabel(loi, "Polygon");
  }

  @Test
  public void testGetLabel_whenCaptionIsPresentAndLoiIsPoint() {
    LocationOfInterest loi =
        new LocationOfInterest(
            FakeData.POINT_OF_INTEREST.getId(),
            FakeData.POINT_OF_INTEREST.getSurvey(),
            FakeData.POINT_OF_INTEREST.getJob(),
            FakeData.POINT_OF_INTEREST.getCustomId(),
            "point caption",
            FakeData.POINT_OF_INTEREST.getCreated(),
            FakeData.POINT_OF_INTEREST.getLastModified(),
            FakeData.POINT_OF_INTEREST.getGeometry());
    assertLabel(loi, "point caption");
  }

  @Test
  public void testGetLabel_whenCaptionIsPresentAndLoiIsPolygon() {
    LocationOfInterest loi =
        new LocationOfInterest(
            FakeData.AREA_OF_INTEREST.getId(),
            FakeData.AREA_OF_INTEREST.getSurvey(),
            FakeData.AREA_OF_INTEREST.getJob(),
            FakeData.AREA_OF_INTEREST.getCustomId(),
            "polygon caption",
            FakeData.AREA_OF_INTEREST.getCreated(),
            FakeData.AREA_OF_INTEREST.getLastModified(),
            FakeData.AREA_OF_INTEREST.getGeometry());
    assertLabel(loi, "polygon caption");
  }

  @Test
  public void testGetSubtitle() {
    LocationOfInterest loi =
        new LocationOfInterest(
            FakeData.POINT_OF_INTEREST.getId(),
            FakeData.POINT_OF_INTEREST.getSurvey(),
            new Job("jobId", "some job"),
            FakeData.POINT_OF_INTEREST.getCustomId(),
            FakeData.POINT_OF_INTEREST.getCaption(),
            FakeData.POINT_OF_INTEREST.getCreated(),
            FakeData.POINT_OF_INTEREST.getLastModified(),
            FakeData.POINT_OF_INTEREST.getGeometry());
    assertThat(loiHelper.getSubtitle(Optional.of(loi))).isEqualTo("Job: some job");
  }

  @Test
  public void testGetSubtitle_whenLoiIsEmpty() {
    assertThat(loiHelper.getSubtitle(Optional.empty())).isEqualTo("");
  }

  private void assertCreatedBy(@Nullable LocationOfInterest loi, String expectedCreatedBy) {
    assertThat(loiHelper.getCreatedBy(Optional.ofNullable(loi))).isEqualTo(expectedCreatedBy);
  }

  private void assertLabel(@Nullable LocationOfInterest loi, String expectedLabel) {
    assertThat(loiHelper.getLabel(Optional.ofNullable(loi))).isEqualTo(expectedLabel);
  }
}
