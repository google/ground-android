/*
 * Copyright 2019 Google LLC
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

package com.google.android.ground.persistence.local.room.entity;

import static com.google.android.ground.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.AreaOfInterest;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.locationofinterest.Point;
import com.google.android.ground.model.locationofinterest.PointOfInterest;
import com.google.android.ground.model.mutation.LocationOfInterestMutation;
import com.google.android.ground.persistence.local.LocalDataConsistencyException;
import com.google.android.ground.persistence.local.room.models.Coordinates;
import com.google.android.ground.persistence.local.room.models.EntityState;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.util.List;
import java8.util.stream.Collectors;

/**
 * Defines how Room persists LOIs in the local db. By default, Room uses the name of object fields
 * and their respective types to determine database column names and types.
 */
@AutoValue
@Entity(
    tableName = "location_of_interest",
    indices = {@Index("survey_id")})
public abstract class LocationOfInterestEntity {
  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "survey_id")
  public abstract String getSurveyId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "job_id")
  public abstract String getJobId();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "geo_json")
  public abstract String getGeoJson();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "polygon_vertices")
  public abstract String getPolygonVertices();

  // TODO: Rename to DeletionState.
  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "state")
  public abstract EntityState getState();

  @CopyAnnotations
  @Nullable
  @Embedded
  public abstract Coordinates getLocation();

  @CopyAnnotations
  @NonNull
  @Embedded(prefix = "created_")
  public abstract AuditInfoEntity getCreated();

  @CopyAnnotations
  @NonNull
  @Embedded(prefix = "modified_")
  public abstract AuditInfoEntity getLastModified();

  @NonNull
  public static LocationOfInterestEntity fromMutation(
      LocationOfInterestMutation mutation, AuditInfo created) {
    AuditInfoEntity authInfo = AuditInfoEntity.fromObject(created);
    LocationOfInterestEntity.Builder entity =
        LocationOfInterestEntity.builder()
            .setId(mutation.getLocationOfInterestId())
            .setSurveyId(mutation.getSurveyId())
            .setJobId(mutation.getJobId())
            .setState(EntityState.DEFAULT)
            .setCreated(authInfo)
            .setLastModified(authInfo);
    mutation.getLocation().map(Coordinates::fromPoint).ifPresent(entity::setLocation);
    entity.setPolygonVertices(formatVertices(mutation.getPolygonVertices()));
    return entity.build();
  }

  public static LocationOfInterestEntity fromLocationOfInterest(
      LocationOfInterest locationOfInterest) {
    LocationOfInterestEntity.Builder entity =
        LocationOfInterestEntity.builder()
            .setId(locationOfInterest.getId())
            .setSurveyId(locationOfInterest.getSurvey().getId())
            .setJobId(locationOfInterest.getJob().getId())
            .setState(EntityState.DEFAULT)
            .setCreated(AuditInfoEntity.fromObject(locationOfInterest.getCreated()))
            .setLastModified(AuditInfoEntity.fromObject(locationOfInterest.getLastModified()));
    if (locationOfInterest instanceof PointOfInterest) {
      entity.setLocation(Coordinates.fromPoint(((PointOfInterest) locationOfInterest).getPoint()));
    } else if (locationOfInterest instanceof AreaOfInterest) {
      entity.setPolygonVertices(
          formatVertices(((AreaOfInterest) locationOfInterest).getVertices()));
    }
    return entity.build();
  }

  public static LocationOfInterest toLocationOfInterest(
      LocationOfInterestEntity locationOfInterestEntity, Survey survey) {
    if (locationOfInterestEntity.getLocation() != null) {
      PointOfInterest.Builder builder =
          PointOfInterest.newBuilder().setPoint(locationOfInterestEntity.getLocation().toPoint());
      fillLocationOfInterest(builder, locationOfInterestEntity, survey);
      return builder.build();
    }

    if (locationOfInterestEntity.getPolygonVertices() != null) {
      AreaOfInterest.Builder builder =
          AreaOfInterest.newBuilder()
              .setVertices(parseVertices(locationOfInterestEntity.getPolygonVertices()));
      fillLocationOfInterest(builder, locationOfInterestEntity, survey);
      return builder.build();
    }

    throw new LocalDataConsistencyException(
        "No geometry data found in location of interest " + locationOfInterestEntity.getId());
  }

  @Nullable
  public static String formatVertices(ImmutableList<Point> vertices) {
    if (vertices.isEmpty()) {
      return null;
    }
    Gson gson = new Gson();
    List<List<Double>> verticesArray =
        stream(vertices)
            .map(point -> ImmutableList.of(point.getLatitude(), point.getLongitude()))
            .collect(Collectors.toList());
    return gson.toJson(verticesArray);
  }

  public static ImmutableList<Point> parseVertices(@Nullable String vertices) {
    if (vertices == null || vertices.isEmpty()) {
      return ImmutableList.of();
    }
    Gson gson = new Gson();
    List<List<Double>> verticesArray =
        gson.fromJson(vertices, new TypeToken<List<List<Double>>>() {}.getType());

    return stream(verticesArray)
        .map(
            vertex ->
                Point.newBuilder().setLatitude(vertex.get(0)).setLongitude(vertex.get(1)).build())
        .collect(toImmutableList());
  }

  public static void fillLocationOfInterest(
      LocationOfInterest.Builder builder,
      LocationOfInterestEntity locationOfInterestEntity,
      Survey survey) {
    String id = locationOfInterestEntity.getId();
    String jobId = locationOfInterestEntity.getJobId();
    Job job =
        survey
            .getJob(jobId)
            .orElseThrow(
                () ->
                    new LocalDataConsistencyException(
                        "Unknown jobId " + jobId + " in location of interest " + id));
    builder
        .setId(id)
        .setSurvey(survey)
        .setJob(job)
        .setCreated(AuditInfoEntity.toObject(locationOfInterestEntity.getCreated()))
        .setLastModified(AuditInfoEntity.toObject(locationOfInterestEntity.getLastModified()));
  }

  public abstract LocationOfInterestEntity.Builder toBuilder();

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static LocationOfInterestEntity create(
      String id,
      String surveyId,
      String jobId,
      String geoJson,
      String polygonVertices,
      EntityState state,
      Coordinates location,
      AuditInfoEntity created,
      AuditInfoEntity lastModified) {
    return builder()
        .setId(id)
        .setSurveyId(surveyId)
        .setJobId(jobId)
        .setGeoJson(geoJson)
        .setPolygonVertices(polygonVertices)
        .setState(state)
        .setLocation(location)
        .setCreated(created)
        .setLastModified(lastModified)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_LocationOfInterestEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setSurveyId(String newSurveyId);

    public abstract Builder setJobId(String newJobId);

    public abstract Builder setGeoJson(@Nullable String newGeoJson);

    public abstract Builder setPolygonVertices(@Nullable String newPolygonVertices);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setLocation(@Nullable Coordinates newLocation);

    public abstract Builder setCreated(AuditInfoEntity newCreated);

    public abstract Builder setLastModified(AuditInfoEntity newLastModified);

    public abstract LocationOfInterestEntity build();
  }
}
