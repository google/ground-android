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

package com.google.android.ground.model;

import com.google.android.ground.model.geometry.Point;
import com.google.android.ground.model.geometry.Polygon;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.task.Field;
import com.google.android.ground.model.task.Field.Type;
import com.google.android.ground.model.task.Task;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.GeoPoint;
import java.util.Date;

/**
 * Helper methods for building model objects for use in tests. Method return builders with required
 * fields set to placeholder values. Rather than depending on these values, tests that test for
 * specific values should explicitly set them in relevant test methods or during test setup.
 */
public class TestModelBuilders {

  public static Survey.Builder newSurvey() {
    return Survey.newBuilder().setId("").setTitle("").setDescription("");
  }

  public static User.Builder newUser() {
    return User.builder().setId("").setEmail("").setDisplayName("");
  }

  public static AuditInfo.Builder newAuditInfo() {
    return AuditInfo.builder().setClientTimestamp(new Date(0)).setUser(newUser().build());
  }

  public static Point newPoint() {
    return new Point(0, 0);
  }

  public static GeoPoint newGeoPoint() {
    return new GeoPoint(0.0, 0.0);
  }

  public static ImmutableList<GeoPoint> newGeoPointPolygonVertices() {
    return ImmutableList.<GeoPoint>builder()
        .add(newGeoPoint())
        .add(newGeoPoint())
        .add(newGeoPoint())
        .build();
  }

  public static ImmutableList<Point> newPolygonVertices() {
    return ImmutableList.<Point>builder().add(newPoint()).add(newPoint()).add(newPoint()).build();
  }

  public static LocationOfInterest.Builder<Point> newPointOfInterest() {
    return LocationOfInterest.<Point>newBuilder()
        .setId("")
        .setSurvey(newSurvey().build())
        .setGeometry(newPoint())
        .setCreated(newAuditInfo().build())
        .setLastModified(newAuditInfo().build());
  }

  public static LocationOfInterest.Builder<Polygon> newPolygonOfInterest() {
    return LocationOfInterest.<Polygon>newBuilder()
        .setId("")
        .setSurvey(newSurvey().build())
        .setGeometry(new Polygon(newPolygonVertices()))
        .setCreated(newAuditInfo().build())
        .setLastModified(newAuditInfo().build());
  }

  public static TermsOfService.Builder newTermsOfService() {
    return TermsOfService.builder().setId("").setText("");
  }

  public static Job.Builder newJob() {
    return Job.newBuilder().setId("").setName("");
  }

  public static Task.Builder newTask() {
    return Task.newBuilder().setId("");
  }

  public static Field.Builder newField() {
    return Field.newBuilder()
        .setId("")
        .setIndex(0)
        .setType(Type.TEXT_FIELD)
        .setLabel("")
        .setRequired(false);
  }
}
