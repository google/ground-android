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

package com.google.android.gnd.ui.home.featuredetails;

import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.system.auth.FakeAuthenticationManager;
import com.google.common.collect.ImmutableMap;
import javax.inject.Inject;

public abstract class BaseMenuVisibilityTest extends BaseHiltTest {

  static final User TEST_USER_OWNER = FakeData.USER.toBuilder().setEmail("user1@gmail.com").build();

  static final User TEST_USER_MANAGER =
      FakeData.USER.toBuilder().setEmail("user2@gmail.com").build();

  static final User TEST_USER_CONTRIBUTOR =
      FakeData.USER.toBuilder().setEmail("user3@gmail.com").build();

  static final User TEST_USER_UNKNOWN =
      FakeData.USER.toBuilder().setEmail("user4@gmail.com").build();

  private static final Survey TEST_SURVEY =
      FakeData.SURVEY.toBuilder()
          .setAcl(
              ImmutableMap.<String, String>builder()
                  .put(TEST_USER_OWNER.getEmail(), "owner")
                  .put(TEST_USER_MANAGER.getEmail(), "survey_organizer")
                  .put(TEST_USER_CONTRIBUTOR.getEmail(), "data_collector")
                  .build())
          .build();

  protected final User user;
  protected final Feature feature;
  protected final boolean visible;

  @Inject FakeAuthenticationManager fakeAuthenticationManager;
  @Inject FeatureDetailsViewModel viewModel;

  public BaseMenuVisibilityTest(User user, Feature feature, boolean visible) {
    this.user = user;
    this.feature = feature;
    this.visible = visible;
  }

  static PointFeature createPointFeature(User user) {
    return FakeData.POINT_FEATURE.toBuilder()
        .setSurvey(TEST_SURVEY)
        .setCreated(AuditInfo.now(user))
        .build();
  }

  static PolygonFeature createPolygonFeature(User user) {
    return FakeData.POLYGON_FEATURE.toBuilder()
        .setSurvey(TEST_SURVEY)
        .setCreated(AuditInfo.now(user))
        .build();
  }
}
