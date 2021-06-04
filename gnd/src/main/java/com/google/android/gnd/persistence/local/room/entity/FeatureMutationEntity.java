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

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.persistence.local.room.models.Coordinates;
import com.google.android.gnd.persistence.local.room.models.MutationEntityType;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Date;
import java8.util.Optional;
import timber.log.Timber;

/**
 * Defines how Room persists feature mutations for remote sync in the local db. By default, Room
 * uses the name of object fields and their respective types to determine database column names and
 * types.
 */
@AutoValue
@Entity(
    tableName = "feature_mutation",
    foreignKeys =
        @ForeignKey(
            entity = FeatureEntity.class,
            parentColumns = "id",
            childColumns = "feature_id",
            onDelete = CASCADE),
    indices = {@Index("feature_id")})
public abstract class FeatureMutationEntity extends MutationEntity {

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "feature_id")
  public abstract String getFeatureId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "layer_id")
  public abstract String getLayerId();

  /** Non-null if the feature's location was updated, null if unchanged. */
  @CopyAnnotations
  @Nullable
  @Embedded
  public abstract Coordinates getNewLocation();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "polygon_vertices")
  public abstract String getPolygonVertices();

  public static FeatureMutationEntity fromMutation(FeatureMutation m) {
    return FeatureMutationEntity.builder()
        .setId(m.getId())
        .setProjectId(m.getProjectId())
        .setFeatureId(m.getFeatureId())
        .setLayerId(m.getLayerId())
        .setNewLocation(m.getNewLocation().map(Coordinates::fromPoint).orElse(null))
        .setPolygonVertices(listToString(m.getNewPolygonVertices().orElse(null)))
        .setType(MutationEntityType.fromMutationType(m.getType()))
        .setRetryCount(m.getRetryCount())
        .setLastError(m.getLastError())
        .setUserId(m.getUserId())
        .setClientTimestamp(m.getClientTimestamp().getTime())
        .build();
  }

  public static String listToString(ImmutableList<Point> vertices) {
    if (vertices == null) {
      Timber.d("vertices are null");
      return null;
    }
    ArrayList<ArrayList<Double>> polygonVertices = new ArrayList<>();
    for (Point vertex : vertices) {
      ArrayList<Double> innerValues = new ArrayList<>();
      innerValues.add(vertex.getLatitude());
      innerValues.add(vertex.getLongitude());
      polygonVertices.add(innerValues);
    }
    Gson gson = new Gson();
    return gson.toJson(polygonVertices);
  }

  public static ImmutableList<Point> stringToList(String vertices) {
    if (vertices == null) {
      Timber.d("vertices are null");
      return null;
    }
    Gson gson = new Gson();
    ArrayList<Point> polygonVertices = new ArrayList<>();
    ArrayList<ArrayList<Double>> verticesArray =
        gson.fromJson(vertices, new TypeToken<ArrayList<ArrayList<Double>>>(){}.getType());
    for (ArrayList<Double> values :verticesArray) {
      polygonVertices.add(Point.newBuilder().setLatitude(values.get(0))
          .setLongitude(values.get(1)).build());
    }
    ImmutableList.Builder<Point> polygonPoints = ImmutableList.builder();
    return polygonPoints.addAll(polygonVertices).build();
  }

  public FeatureMutation toMutation() {
    return FeatureMutation.builder()
        .setId(getId())
        .setProjectId(getProjectId())
        .setFeatureId(getFeatureId())
        .setLayerId(getLayerId())
        .setNewLocation(Optional.ofNullable(getNewLocation()).map(Coordinates::toPoint))
        .setNewPolygonVertices(Optional.ofNullable(stringToList(getPolygonVertices())))
        .setType(getType().toMutationType())
        .setRetryCount(getRetryCount())
        .setLastError(getLastError())
        .setUserId(getUserId())
        .setClientTimestamp(new Date(getClientTimestamp()))
        .build();
  }

  // Boilerplate generated using Android Studio AutoValue plugin:

  public static FeatureMutationEntity create(
      @Nullable Long id,
      String projectId,
      String featureId,
      String layerId,
      MutationEntityType type,
      Coordinates newLocation,
      String polygonVertices,
      long retryCount,
      @Nullable String lastError,
      String userId,
      long clientTimestamp) {
    return builder()
        .setId(id)
        .setProjectId(projectId)
        .setFeatureId(featureId)
        .setLayerId(layerId)
        .setType(type)
        .setNewLocation(newLocation)
        .setPolygonVertices(polygonVertices)
        .setRetryCount(retryCount)
        .setLastError(lastError)
        .setUserId(userId)
        .setClientTimestamp(clientTimestamp)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_FeatureMutationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder extends MutationEntity.Builder<Builder> {

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setLayerId(String newLayerId);

    public abstract Builder setNewLocation(@Nullable Coordinates newNewLocation);

    public abstract Builder setPolygonVertices(@Nullable String  newPolygonVertices);

    public abstract FeatureMutationEntity build();
  }
}
