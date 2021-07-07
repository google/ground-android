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

package com.google.android.gnd.persistence.local.room.entity;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.GeoJsonFeature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.local.LocalDataConsistencyException;
import com.google.android.gnd.persistence.local.room.models.Coordinates;
import com.google.android.gnd.persistence.local.room.models.EntityState;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java8.util.stream.Collectors;
import timber.log.Timber;

/**
 * Defines how Room persists features in the local db. By default, Room uses the name of object
 * fields and their respective types to determine database column names and types.
 */
@AutoValue
@Entity(
    tableName = "feature",
    indices = {@Index("project_id")})
public abstract class FeatureEntity {
  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "project_id")
  public abstract String getProjectId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "layer_id")
  public abstract String getLayerId();

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
  public static FeatureEntity fromMutation(FeatureMutation mutation, AuditInfo created) {
    AuditInfoEntity authInfo = AuditInfoEntity.fromObject(created);
    FeatureEntity.Builder entity =
        FeatureEntity.builder()
            .setId(mutation.getFeatureId())
            .setProjectId(mutation.getProjectId())
            .setLayerId(mutation.getLayerId())
            .setState(EntityState.DEFAULT)
            .setCreated(authInfo)
            .setLastModified(authInfo);
    mutation.getNewLocation().map(Coordinates::fromPoint).ifPresent(entity::setLocation);
    mutation.getNewPolygonVertices().map(FeatureEntity::listToString)
        .ifPresent(entity::setPolygonVertices);
    return entity.build();
  }

  public static FeatureEntity fromFeature(Feature feature) {
    FeatureEntity.Builder entity =
        FeatureEntity.builder()
            .setId(feature.getId())
            .setProjectId(feature.getProject().getId())
            .setLayerId(feature.getLayer().getId())
            .setState(EntityState.DEFAULT)
            .setCreated(AuditInfoEntity.fromObject(feature.getCreated()))
            .setLastModified(AuditInfoEntity.fromObject(feature.getLastModified()));
    if (feature instanceof PointFeature) {
      entity.setLocation(Coordinates.fromPoint(((PointFeature) feature).getPoint()));
    } else if (feature instanceof GeoJsonFeature) {
      entity.setGeoJson(((GeoJsonFeature) feature).getGeoJsonString());
    } else if (feature instanceof PolygonFeature) {
      entity.setPolygonVertices(listToString(((PolygonFeature) feature).getVertices()));
    }
    return entity.build();
  }

  public static Feature toFeature(FeatureEntity featureEntity, Project project) {
    if (featureEntity.getGeoJson() != null) {
      GeoJsonFeature.Builder builder =
          GeoJsonFeature.newBuilder().setGeoJsonString(featureEntity.getGeoJson());
      fillFeature(builder, featureEntity, project);
      return builder.build();
    }

    if (featureEntity.getLocation() != null) {
      PointFeature.Builder builder =
          PointFeature.newBuilder().setPoint(featureEntity.getLocation().toPoint());
      fillFeature(builder, featureEntity, project);
      return builder.build();
    }

    if (featureEntity.getPolygonVertices() != null) {
      PolygonFeature.Builder builder =
          PolygonFeature.builder().setVertices(stringToList(featureEntity.getPolygonVertices()));
      fillFeature(builder, featureEntity, project);
      return builder.build();
    }

    throw new LocalDataConsistencyException(
        "No geometry data found in feature " + featureEntity.getId());
  }

  @Nullable
  public static String listToString(ImmutableList<Point> vertices) {
    if (vertices == null || vertices.isEmpty()) {
      Timber.d("vertices are null");
      return null;
    }
    Gson gson = new Gson();
    List<List<Double>> verticesArray = stream(vertices)
        .map(point -> ImmutableList.of(point.getLatitude(),
            point.getLongitude())).collect(Collectors.toList());
    return gson.toJson(verticesArray);
  }

  @Nullable
  public static ImmutableList<Point> stringToList(String vertices) {
    if (vertices == null || vertices.isEmpty()) {
      Timber.d("vertices are null");
      return null;
    }
    Gson gson = new Gson();
    ArrayList<ArrayList<Double>> verticesArray =
        gson.fromJson(vertices, new TypeToken<ArrayList<ArrayList<Double>>>(){}.getType());

    return stream(verticesArray).map(vertice -> Point.newBuilder()
        .setLatitude(vertice.get(0))
        .setLongitude(vertice.get(1))
        .build())
        .collect(toImmutableList());
  }


  public static void fillFeature(
      Feature.Builder builder, FeatureEntity featureEntity, Project project) {
    String id = featureEntity.getId();
    String layerId = featureEntity.getLayerId();
    Layer layer =
        project
            .getLayer(layerId)
            .orElseThrow(
                () ->
                    new LocalDataConsistencyException(
                        "Unknown layerId " + layerId + " in feature " + id));
    builder
        .setId(id)
        .setProject(project)
        .setLayer(layer)
        .setCreated(AuditInfoEntity.toObject(featureEntity.getCreated()))
        .setLastModified(AuditInfoEntity.toObject(featureEntity.getLastModified()));
  }

  public abstract FeatureEntity.Builder toBuilder();

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static FeatureEntity create(
      String id,
      String projectId,
      String layerId,
      String geoJson,
      String polygonVertices,
      EntityState state,
      Coordinates location,
      AuditInfoEntity created,
      AuditInfoEntity lastModified) {
    return builder()
        .setId(id)
        .setProjectId(projectId)
        .setLayerId(layerId)
        .setGeoJson(geoJson)
        .setPolygonVertices(polygonVertices)
        .setState(state)
        .setLocation(location)
        .setCreated(created)
        .setLastModified(lastModified)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_FeatureEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setProjectId(String newProjectId);

    public abstract Builder setLayerId(String newLayerId);

    public abstract Builder setGeoJson(@Nullable String newGeoJson);

    public abstract Builder setPolygonVertices(@Nullable String newPolygonVertices);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setLocation(@Nullable Coordinates newLocation);

    public abstract Builder setCreated(AuditInfoEntity newCreated);

    public abstract Builder setLastModified(AuditInfoEntity newLastModified);

    public abstract FeatureEntity build();
  }
}
