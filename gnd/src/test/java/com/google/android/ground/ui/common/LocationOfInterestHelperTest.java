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

import static com.google.android.ground.FakeData.GEO_JSON_FEATURE;
import static com.google.android.ground.FakeData.JOB;
import static com.google.android.ground.FakeData.POINT_FEATURE;
import static com.google.android.ground.FakeData.POLYGON_FEATURE;
import static com.google.android.ground.FakeData.USER;
import static com.google.common.truth.Truth.assertThat;

import com.google.android.ground.BaseHiltTest;
import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.locationofinterest.GeoJsonLocationOfInterest;
import com.google.android.ground.model.locationofinterest.PointOfInterest;
import com.google.android.ground.model.locationofinterest.PolygonOfInterest;
import dagger.hilt.android.testing.HiltAndroidTest;
import java8.util.Optional;
import javax.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;
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
        POINT_FEATURE.toBuilder()
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
    PointOfInterest feature = POINT_FEATURE.toBuilder().setCaption("").build();
    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("Point");
  }

  @Test
  public void testGetLabel_whenCaptionIsEmptyAndFeatureIsPolygon() {
    PolygonOfInterest feature = POLYGON_FEATURE.toBuilder().setCaption("").build();
    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("Polygon");
  }

  @Test
  public void testGetLabel_whenCaptionIsEmptyAndFeatureIsGeoJson() throws JSONException {
    JSONObject propertiesJson = new JSONObject().put("id", "foo id").put("caption", "");
    JSONObject jsonObject = new JSONObject().put("properties", propertiesJson);
    GeoJsonLocationOfInterest feature =
        GEO_JSON_FEATURE.toBuilder().setGeoJsonString(jsonObject.toString()).build();

    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("Polygon foo id");
  }

  @Test
  public void testGetLabel_whenCaptionIsPresentAndFeatureIsPoint() {
    PointOfInterest feature = POINT_FEATURE.toBuilder().setCaption("point caption").build();
    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("point caption");
  }

  @Test
  public void testGetLabel_whenCaptionIsPresentAndFeatureIsPolygon() {
    PolygonOfInterest feature = POLYGON_FEATURE.toBuilder().setCaption("polygon caption").build();
    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("polygon caption");
  }

  @Test
  public void testGetLabel_whenCaptionIsPresentAndFeatureIsGeoJson() throws JSONException {
    JSONObject propertiesJson = new JSONObject().put("id", "foo id").put("caption", "foo caption");
    JSONObject jsonObject = new JSONObject().put("properties", propertiesJson);
    GeoJsonLocationOfInterest feature =
        GEO_JSON_FEATURE.toBuilder().setGeoJsonString(jsonObject.toString()).build();

    assertThat(featureHelper.getLabel(Optional.of(feature))).isEqualTo("foo caption");
  }

  @Test
  public void testGetSubtitle() {
    PointOfInterest feature =
        POINT_FEATURE.toBuilder().setJob(JOB.toBuilder().setName("some job").build()).build();

    assertThat(featureHelper.getSubtitle(Optional.of(feature))).isEqualTo("Job: some job");
  }

  @Test
  public void testGetSubtitle_whenFeatureIsEmpty() {
    assertThat(featureHelper.getSubtitle(Optional.empty())).isEqualTo("");
  }
}
