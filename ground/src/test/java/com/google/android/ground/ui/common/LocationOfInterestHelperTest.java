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

import static com.google.android.ground.test.FakeData.AREA_OF_INTEREST;
import static com.google.android.ground.test.FakeData.POINT_OF_INTEREST;
import static com.google.android.ground.test.FakeData.USER;
import static com.google.common.truth.Truth.assertThat;

import com.google.android.ground.BaseHiltTest;
import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.AreaOfInterest;
import com.google.android.ground.model.locationofinterest.PointOfInterest;
import dagger.hilt.android.testing.HiltAndroidTest;
import java8.util.Optional;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class LocationOfInterestHelperTest extends BaseHiltTest {

  @Inject LocationOfInterestHelper featureHelper;

  @Test
  public void testGetCreatedBy() {
    PointOfInterest feature =
        POINT_OF_INTEREST.toBuilder()
            .setCreated(AuditInfo.now(USER.toBuilder().setDisplayName("Test User").build()))
            .build();

    assertThat(featureHelper.getCreatedBy(Optional.of(feature))).isEqualTo("Added by Test User");
  }

  @Test
  public void testGetCreatedBy_whenFeatureIsEmpty() {
    assertThat(featureHelper.getCreatedBy(Optional.empty())).isEqualTo("");
  }

  @Test
  public void testGetLabel_whenFeatureIsEmpty() {
    assertThat(featureHelper.getLabel(Optional.empty())).isEqualTo("");
  }

  @Test
  public void testGetLabel_whenCaptionIsEmptyAndFeatureIsPoint() {
    PointOfInterest feature = POINT_OF_INTEREST.toBuilder().setCaption("").build();
    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("Point");
  }

  @Test
  public void testGetLabel_whenCaptionIsEmptyAndFeatureIsPolygon() {
    AreaOfInterest feature = AREA_OF_INTEREST.toBuilder().setCaption("").build();
    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("Polygon");
  }

  @Test
  public void testGetLabel_whenCaptionIsPresentAndFeatureIsPoint() {
    PointOfInterest feature = POINT_OF_INTEREST.toBuilder().setCaption("point caption").build();
    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("point caption");
  }

  @Test
  public void testGetLabel_whenCaptionIsPresentAndFeatureIsPolygon() {
    AreaOfInterest feature = AREA_OF_INTEREST.toBuilder().setCaption("polygon caption").build();
    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("polygon caption");
  }

  @Test
  public void testGetSubtitle() {
    PointOfInterest feature =
        POINT_OF_INTEREST.toBuilder().setJob(new Job("jobId", "some job")).build();

    assertThat(featureHelper.getSubtitle(Optional.of(feature))).isEqualTo("Job: some job");
  }

  @Test
  public void testGetSubtitle_whenFeatureIsEmpty() {
    assertThat(featureHelper.getSubtitle(Optional.empty())).isEqualTo("");
  }
}
