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
package com.google.android.gnd.persistence.local.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.model.basemap.Area;
import com.google.auto.value.AutoValue;

/** Represents a {@link Area} in the local data store. */
@AutoValue
@Entity(tableName = "area")
public abstract class AreaEntity {
  public static Area toArea(AreaEntity areaEntity) {
    LatLng northEast = new LatLng(areaEntity.getNorth(), areaEntity.getEast());
    LatLng southWest = new LatLng(areaEntity.getSouth(), areaEntity.getWest());
    LatLngBounds bounds = new LatLngBounds(northEast, southWest);

    Area.Builder area =
        Area.newBuilder()
            .setId(areaEntity.getId())
            .setBounds(bounds)
            .setState(toAreaState(areaEntity.getState()));
    return area.build();
  }

  private static Area.State toAreaState(AreaEntityState state) {
    switch (state) {
      case PENDING:
        return Area.State.PENDING;
      case IN_PROGRESS:
        return Area.State.IN_PROGRESS;
      case DOWNLOADED:
        return Area.State.DOWNLOADED;
      case FAILED:
        return Area.State.FAILED;
      default:
        throw new IllegalArgumentException("Unknown area state: " + state);
    }
  }

  public static AreaEntity fromArea(Area area) {
    AreaEntity.Builder entity =
        AreaEntity.builder()
            .setId(area.getId())
            .setState(toEntityState(area.getState()))
            .setNorth(area.getBounds().northeast.latitude)
            .setEast(area.getBounds().northeast.longitude)
            .setSouth(area.getBounds().southwest.latitude)
            .setWest(area.getBounds().southwest.longitude);
    return entity.build();
  }

  private static AreaEntityState toEntityState(Area.State state) {
    switch (state) {
      case PENDING:
        return AreaEntityState.PENDING;
      case IN_PROGRESS:
        return AreaEntityState.IN_PROGRESS;
      case FAILED:
        return AreaEntityState.FAILED;
      case DOWNLOADED:
        return AreaEntityState.DOWNLOADED;
      default:
        return AreaEntityState.UNKNOWN;
    }
  }

  public static AreaEntity create(String id, AreaEntityState state, Double north, Double east, Double south, Double west) {
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
    return new AutoValue_AreaEntity.Builder();
  }

  @AutoValue.CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "state")
  public abstract AreaEntityState getState();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "north")
  public abstract Double getNorth();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "south")
  public abstract Double getSouth();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "east")
  public abstract Double getEast();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "west")
  public abstract Double getWest();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setState(AreaEntityState newState);

    public abstract Builder setNorth(Double coordinate);

    public abstract Builder setSouth(Double coordinate);

    public abstract Builder setEast(Double coordinate);

    public abstract Builder setWest(Double coordinate);

    public abstract AreaEntity build();
  }
}
