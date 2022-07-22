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
import com.google.android.ground.model.geometry.Point;
import com.google.android.ground.model.geometry.Polygon;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.job.Job.Builder;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
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

  public static final Job JOB = newJob().build();

  public static Builder newJob() {
    return Job.newBuilder().setId("JOB").setName("Job");
  }

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

  public static final LocationOfInterest<Point> POINT_OF_INTEREST =
      LocationOfInterest.<Point>newBuilder()
          .setId("loi id")
          .setSurvey(SURVEY)
          .setJob(JOB)
          .setGeometry(new Point(0.0, 0.0))
          .setCreated(AuditInfo.now(USER))
          .setLastModified(AuditInfo.now(USER))
          .build();

  public static final ImmutableList<Point> VERTICES =
      ImmutableList.of(
          new Point(0.0, 0.0), new Point(10.0, 10.0), new Point(20.0, 20.0), new Point(0.0, 0.0));

  public static final LocationOfInterest<Polygon> AREA_OF_INTEREST =
      LocationOfInterest.<Polygon>newBuilder()
          .setId("loi id")
          .setSurvey(SURVEY)
          .setJob(JOB)
          .setGeometry(new Polygon(VERTICES))
          .setCreated(AuditInfo.now(USER))
          .setLastModified(AuditInfo.now(USER))
          .build();

  public static final Point POINT = new Point(42.0, 18.0);
}
