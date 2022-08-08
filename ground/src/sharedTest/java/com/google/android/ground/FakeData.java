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

package com.google.android.ground;

import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.TermsOfService;
import com.google.android.ground.model.User;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.AreaOfInterest;
import com.google.android.ground.model.locationofinterest.Point;
import com.google.android.ground.model.locationofinterest.PointOfInterest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Shared test data constants. Tests are expected to override existing or set missing values when
 * the specific value is relevant to the test.
 */
public class FakeData {
  // TODO: Replace constants with calls to newFoo() methods.
  public static final TermsOfService TERMS_OF_SERVICE =
      TermsOfService.builder()
          .setId("TERMS_OF_SERVICE")
          .setText("Fake Terms of Service text")
          .build();

  public static final Job JOB = new Job("JOB", "Job");

  public static final User USER =
      User.builder().setId("user_id").setEmail("user@gmail.com").setDisplayName("User").build();

  public static final User USER_2 =
      User.builder().setId("user_id_2").setEmail("user2@gmail.com").setDisplayName("User2").build();

  public static final Survey SURVEY = newSurvey().build();

  public static Survey.Builder newSurvey() {
    return Survey.newBuilder()
        .setId("SURVEY")
        .setTitle("Survey title")
        .setDescription("Test survey description")
        .setAcl(ImmutableMap.of(FakeData.USER.getEmail(), "data_collector"));
  }

  public static final PointOfInterest POINT_OF_INTEREST =
      PointOfInterest.newBuilder()
          .setId("loi id")
          .setSurvey(SURVEY)
          .setJob(JOB)
          .setPoint(Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build())
          .setCreated(AuditInfo.now(USER))
          .setLastModified(AuditInfo.now(USER))
          .build();

  public static final ImmutableList<Point> VERTICES =
      ImmutableList.of(
          Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build(),
          Point.newBuilder().setLatitude(10.0).setLongitude(10.0).build(),
          Point.newBuilder().setLatitude(20.0).setLongitude(20.0).build());

  public static final AreaOfInterest AREA_OF_INTEREST =
      AreaOfInterest.newBuilder()
          .setId("loi id")
          .setSurvey(SURVEY)
          .setVertices(VERTICES)
          .setCreated(AuditInfo.now(USER))
          .setLastModified(AuditInfo.now(USER))
          .setJob(JOB)
          .build();

  public static final Point POINT = Point.newBuilder().setLatitude(42.0).setLongitude(18.0).build();
}
