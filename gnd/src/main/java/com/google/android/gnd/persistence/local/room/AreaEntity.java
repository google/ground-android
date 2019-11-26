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

/**
 * Represents a {@link Area} in the local data store.
 */
@AutoValue
@Entity(tableName = "area")
public abstract class AreaEntity {
  public static Area toArea(AreaEntity areaEntity) {
    Area.Builder area =
        Area.newBuilder()
            .setBounds(boundsFromString(areaEntity.getId()))
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

  private static LatLngBounds boundsFromString(String id) {
    String[] coords = id.split(",");
    LatLng NE = new LatLng(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
    LatLng SW = new LatLng(Double.parseDouble(coords[2]), Double.parseDouble(coords[3]));
    return new LatLngBounds(NE, SW);
  }

  public static AreaEntity fromArea(Area area) {
    AreaEntity.Builder entity =
        AreaEntity.builder().setId(area.getId()).setState(toEntityState(area.getState()));
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

  public static AreaEntity create(String id, AreaEntityState state) {
    return builder().setId(id).setState(state).build();
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

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String newId);

    public abstract Builder setState(AreaEntityState newState);

    public abstract AreaEntity build();
  }
}
