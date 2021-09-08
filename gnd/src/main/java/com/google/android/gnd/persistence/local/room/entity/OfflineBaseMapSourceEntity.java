/*
 * Copyright 2020 Google LLC
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
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.basemap.OfflineBaseMapSource;
import com.google.android.gnd.model.basemap.OfflineBaseMapSource.OfflineBaseMapSourceType;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import java.net.MalformedURLException;
import java.net.URL;

@AutoValue
@Entity(
    tableName = "offline_base_map_source",
    foreignKeys =
        @ForeignKey(
            entity = ProjectEntity.class,
            parentColumns = "id",
            childColumns = "project_id", // NOPMD
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("project_id")}) // NOPMD
public abstract class OfflineBaseMapSourceEntity {

  public static OfflineBaseMapSource toModel(OfflineBaseMapSourceEntity source)
      throws MalformedURLException {
    return OfflineBaseMapSource.builder()
        .setUrl(new URL(source.getUrl()))
        .setType(entityToModelType(source))
        .build();
  }

  @CopyAnnotations
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id") // NOPMD
  @Nullable
  public abstract Integer getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "project_id") // NOPMD
  public abstract String getProjectId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "url")
  public abstract String getUrl();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "type")
  public abstract OfflineBaseMapSourceEntityType getType();

  public enum OfflineBaseMapSourceEntityType {
    GEOJSON,
    IMAGE,
    UNKNOWN
  }

  private static OfflineBaseMapSourceEntityType modelToEntityType(OfflineBaseMapSource source) {
    switch (source.getType()) {
      case TILED_WEB_MAP:
        return OfflineBaseMapSourceEntityType.IMAGE;
      case MBTILES_FOOTPRINTS:
        return OfflineBaseMapSourceEntityType.GEOJSON;
      default:
        return OfflineBaseMapSourceEntityType.UNKNOWN;
    }
  }

  private static OfflineBaseMapSourceType entityToModelType(OfflineBaseMapSourceEntity source) {
    switch (source.getType()) {
      case IMAGE:
        return OfflineBaseMapSourceType.TILED_WEB_MAP;
      case GEOJSON:
        return OfflineBaseMapSourceType.MBTILES_FOOTPRINTS;
      default:
        return OfflineBaseMapSourceType.UNKNOWN;
    }
  }

  public static OfflineBaseMapSourceEntity fromModel(
      String projectId, OfflineBaseMapSource source) {

    return OfflineBaseMapSourceEntity.builder()
        .setProjectId(projectId)
        .setUrl(source.getUrl().toString())
        .setType(modelToEntityType(source))
        .build();
  }

  public static OfflineBaseMapSourceEntity create(
      @Nullable Integer id, String projectId, String url, OfflineBaseMapSourceEntityType type) {
    return builder().setId(id).setProjectId(projectId).setUrl(url).setType(type).build();
  }

  public static Builder builder() {
    return new AutoValue_OfflineBaseMapSourceEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(@Nullable Integer newId);

    public abstract Builder setProjectId(@NonNull String newProjectId);

    public abstract Builder setUrl(@NonNull String newUrl);

    public abstract Builder setType(OfflineBaseMapSourceEntityType type);

    public abstract OfflineBaseMapSourceEntity build();
  }
}
