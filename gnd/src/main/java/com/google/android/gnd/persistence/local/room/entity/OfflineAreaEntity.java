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
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.persistence.local.room.models.OfflineAreaEntityState;
import com.google.auto.value.AutoValue;

/** Represents a {@link OfflineArea} in the local data store. */
@AutoValue
@Entity(tableName = "offline_area")
public abstract class OfflineAreaEntity {
  @AutoValue.CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "state")
  public abstract OfflineAreaEntityState getState();

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

  public static OfflineArea toArea(OfflineAreaEntity offlineAreaEntity) {
    LatLng northEast = new LatLng(offlineAreaEntity.getNorth(), offlineAreaEntity.getEast());
    LatLng southWest = new LatLng(offlineAreaEntity.getSouth(), offlineAreaEntity.getWest());
    LatLngBounds bounds = new LatLngBounds(northEast, southWest);

    return OfflineArea.newBuilder()
        .setId(offlineAreaEntity.getId())
        .setBounds(bounds)
        .setState(toAreaState(offlineAreaEntity.getState()))
        .build();
  }

  private static OfflineArea.State toAreaState(OfflineAreaEntityState state) {
    switch (state) {
      case PENDING:
        return OfflineArea.State.PENDING;
      case IN_PROGRESS:
        return OfflineArea.State.IN_PROGRESS;
      case DOWNLOADED:
        return OfflineArea.State.DOWNLOADED;
      case FAILED:
        return OfflineArea.State.FAILED;
      default:
        throw new IllegalArgumentException("Unknown area state: " + state);
    }
  }

  public static OfflineAreaEntity fromArea(OfflineArea offlineArea) {
    OfflineAreaEntity.Builder entity =
        OfflineAreaEntity.builder()
            .setId(offlineArea.getId())
            .setState(toEntityState(offlineArea.getState()))
            .setNorth(offlineArea.getBounds().northeast.latitude)
            .setEast(offlineArea.getBounds().northeast.longitude)
            .setSouth(offlineArea.getBounds().southwest.latitude)
            .setWest(offlineArea.getBounds().southwest.longitude);
    return entity.build();
  }

  private static OfflineAreaEntityState toEntityState(OfflineArea.State state) {
    switch (state) {
      case PENDING:
        return OfflineAreaEntityState.PENDING;
      case IN_PROGRESS:
        return OfflineAreaEntityState.IN_PROGRESS;
      case FAILED:
        return OfflineAreaEntityState.FAILED;
      case DOWNLOADED:
        return OfflineAreaEntityState.DOWNLOADED;
      default:
        return OfflineAreaEntityState.UNKNOWN;
    }
  }

  public static OfflineAreaEntity create(
      String id,
      OfflineAreaEntityState state,
      double north,
      double east,
      double south,
      double west) {
    return builder()
        .setId(id)
        .setState(state)
        .setNorth(north)
        .setEast(east)
        .setSouth(south)
        .setWest(west)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_OfflineAreaEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setState(OfflineAreaEntityState newState);

    public abstract Builder setNorth(double coordinate);

    public abstract Builder setSouth(double coordinate);

    public abstract Builder setEast(double coordinate);

    public abstract Builder setWest(double coordinate);

    public abstract OfflineAreaEntity build();
  }
}
