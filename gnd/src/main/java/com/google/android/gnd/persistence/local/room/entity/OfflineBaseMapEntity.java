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

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.model.basemap.OfflineBaseMap;
import com.google.android.gnd.persistence.local.room.models.OfflineBaseMapEntityState;
import com.google.auto.value.AutoValue;

/** Represents a {@link OfflineBaseMap} in the local data store. */
@AutoValue
@Entity(tableName = "offline_base_map")
public abstract class OfflineBaseMapEntity {
  @AutoValue.CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "name")
  public abstract String getName();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "state")
  public abstract OfflineBaseMapEntityState getState();

  @AutoValue.CopyAnnotations
  @ColumnInfo(name = "north")
  public abstract double getNorth();

  @AutoValue.CopyAnnotations
  @ColumnInfo(name = "south")
  public abstract double getSouth();

  @AutoValue.CopyAnnotations
  @ColumnInfo(name = "east")
  public abstract double getEast();

  @AutoValue.CopyAnnotations
  @ColumnInfo(name = "west")
  public abstract double getWest();

  @NonNull
  public static OfflineBaseMap toArea(@NonNull OfflineBaseMapEntity offlineBaseMapEntity) {
    LatLng northEast = new LatLng(offlineBaseMapEntity.getNorth(), offlineBaseMapEntity.getEast());
    LatLng southWest = new LatLng(offlineBaseMapEntity.getSouth(), offlineBaseMapEntity.getWest());
    LatLngBounds bounds = new LatLngBounds(southWest, northEast);

    return OfflineBaseMap.newBuilder()
        .setId(offlineBaseMapEntity.getId())
        .setBounds(bounds)
        .setState(toAreaState(offlineBaseMapEntity.getState()))
        .setName(offlineBaseMapEntity.getName())
        .build();
  }

  @NonNull
  private static OfflineBaseMap.State toAreaState(@NonNull OfflineBaseMapEntityState state) {
    switch (state) {
      case PENDING:
        return OfflineBaseMap.State.PENDING;
      case IN_PROGRESS:
        return OfflineBaseMap.State.IN_PROGRESS;
      case DOWNLOADED:
        return OfflineBaseMap.State.DOWNLOADED;
      case FAILED:
        return OfflineBaseMap.State.FAILED;
      default:
        throw new IllegalArgumentException("Unknown area state: " + state);
    }
  }

  @NonNull
  public static OfflineBaseMapEntity fromArea(@NonNull OfflineBaseMap offlineBaseMap) {
    OfflineBaseMapEntity.Builder entity =
        OfflineBaseMapEntity.builder()
            .setId(offlineBaseMap.getId())
            .setState(toEntityState(offlineBaseMap.getState()))
            .setName(offlineBaseMap.getName())
            .setNorth(offlineBaseMap.getBounds().northeast.latitude)
            .setEast(offlineBaseMap.getBounds().northeast.longitude)
            .setSouth(offlineBaseMap.getBounds().southwest.latitude)
            .setWest(offlineBaseMap.getBounds().southwest.longitude);
    return entity.build();
  }

  @NonNull
  private static OfflineBaseMapEntityState toEntityState(@NonNull OfflineBaseMap.State state) {
    switch (state) {
      case PENDING:
        return OfflineBaseMapEntityState.PENDING;
      case IN_PROGRESS:
        return OfflineBaseMapEntityState.IN_PROGRESS;
      case FAILED:
        return OfflineBaseMapEntityState.FAILED;
      case DOWNLOADED:
        return OfflineBaseMapEntityState.DOWNLOADED;
      default:
        return OfflineBaseMapEntityState.UNKNOWN;
    }
  }

  @NonNull
  public static OfflineBaseMapEntity create(
      String id,
      String name,
      OfflineBaseMapEntityState state,
      double north,
      double east,
      double south,
      double west) {
    return builder()
        .setId(id)
        .setName(name)
        .setState(state)
        .setNorth(north)
        .setEast(east)
        .setSouth(south)
        .setWest(west)
        .build();
  }

  @NonNull
  public static Builder builder() {
    return new AutoValue_OfflineBaseMapEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @NonNull
    public abstract Builder setId(String newId);

    @NonNull
    public abstract Builder setName(String newName);

    @NonNull
    public abstract Builder setState(OfflineBaseMapEntityState newState);

    @NonNull
    public abstract Builder setNorth(double coordinate);

    @NonNull
    public abstract Builder setSouth(double coordinate);

    @NonNull
    public abstract Builder setEast(double coordinate);

    @NonNull
    public abstract Builder setWest(double coordinate);

    @NonNull
    public abstract OfflineBaseMapEntity build();
  }
}
