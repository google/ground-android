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

package com.google.android.ground.ui.home.locationofinterestdetails;

import com.google.android.ground.BaseHiltTest;
import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.User;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sharedtest.FakeData;
import com.sharedtest.system.auth.FakeAuthenticationManager;
import javax.inject.Inject;

public abstract class BaseMenuVisibilityTest extends BaseHiltTest {

  static final User TEST_USER_OWNER = new User("user1", "user@gmail.com", "user 1");

  static final User TEST_USER_MANAGER = new User("user2", "user2@gmail.com", "user 2");

  static final User TEST_USER_CONTRIBUTOR = new User("user2", "user3@gmail.com", "user 3");

  static final User TEST_USER_UNKNOWN = new User("user2", "user4@gmail.com", "user 4");

  // TODO: Once migrated to kotlin, use FakeData and only replace the fields which are to be tested
  static final Survey TEST_SURVEY =
      new Survey(
          "SURVEY",
          "Survey title",
          "Test survey description",
          ImmutableMap.of(),
          ImmutableList.of(),
          ImmutableMap.<String, String>builder()
              .put(TEST_USER_OWNER.getEmail(), "owner")
              .put(TEST_USER_MANAGER.getEmail(), "survey-organizer")
              .put(TEST_USER_CONTRIBUTOR.getEmail(), "data-collector")
              .build());

  protected final User user;
  protected final LocationOfInterest locationOfInterest;
  protected final boolean visible;

  @Inject FakeAuthenticationManager fakeAuthenticationManager;
  @Inject LocationOfInterestDetailsViewModel viewModel;

  protected BaseMenuVisibilityTest(
      User user, LocationOfInterest locationOfInterest, boolean visible) {
    this.user = user;
    this.locationOfInterest = locationOfInterest;
    this.visible = visible;
  }

  static LocationOfInterest createPointOfInterest(User user) {
    return new LocationOfInterest(
        FakeData.LOCATION_OF_INTEREST.getId(),
        TEST_SURVEY.getId(),
        FakeData.LOCATION_OF_INTEREST.getJob(),
        FakeData.LOCATION_OF_INTEREST.getCustomId(),
        FakeData.LOCATION_OF_INTEREST.getCaption(),
        new AuditInfo(user),
        FakeData.LOCATION_OF_INTEREST.getLastModified(),
        FakeData.LOCATION_OF_INTEREST.getGeometry());
  }

  static LocationOfInterest createAreaOfInterest(User user) {
    return new LocationOfInterest(
        FakeData.AREA_OF_INTEREST.getId(),
        TEST_SURVEY.getId(),
        FakeData.AREA_OF_INTEREST.getJob(),
        FakeData.AREA_OF_INTEREST.getCustomId(),
        FakeData.AREA_OF_INTEREST.getCaption(),
        new AuditInfo(user),
        FakeData.AREA_OF_INTEREST.getLastModified(),
        FakeData.AREA_OF_INTEREST.getGeometry());
  }
}
