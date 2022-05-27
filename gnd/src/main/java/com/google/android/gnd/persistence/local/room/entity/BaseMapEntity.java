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
import com.google.android.gnd.model.basemap.BaseMap;
import com.google.android.gnd.model.basemap.BaseMap.BaseMapType;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import java.net.MalformedURLException;
import java.net.URL;

@AutoValue
@Entity(
    tableName = "offline_base_map_source",
    foreignKeys =
        @ForeignKey(
            entity = SurveyEntity.class,
            parentColumns = "id",
            childColumns = "survey_id", // NOPMD
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("survey_id")}) // NOPMD
public abstract class BaseMapEntity {

  public static BaseMap toModel(BaseMapEntity source)
      throws MalformedURLException {
    return BaseMap.builder()
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
  @ColumnInfo(name = "survey_id") // NOPMD
  public abstract String getSurveyId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "url")
  public abstract String getUrl();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "type")
  public abstract BaseMapEntityType getType();

  public enum BaseMapEntityType {
    GEOJSON,
    IMAGE,
    UNKNOWN
  }

  private static BaseMapEntityType modelToEntityType(BaseMap source) {
    switch (source.getType()) {
      case TILED_WEB_MAP:
        return BaseMapEntityType.IMAGE;
      case MBTILES_FOOTPRINTS:
        return BaseMapEntityType.GEOJSON;
      default:
        return BaseMapEntityType.UNKNOWN;
    }
  }

  private static BaseMapType entityToModelType(BaseMapEntity source) {
    switch (source.getType()) {
      case IMAGE:
        return BaseMapType.TILED_WEB_MAP;
      case GEOJSON:
        return BaseMapType.MBTILES_FOOTPRINTS;
      default:
        return BaseMapType.UNKNOWN;
    }
  }

  public static BaseMapEntity fromModel(
      String surveyId, BaseMap source) {

    return BaseMapEntity.builder()
        .setSurveyId(surveyId)
        .setUrl(source.getUrl().toString())
        .setType(modelToEntityType(source))
        .build();
  }

  public static BaseMapEntity create(
      @Nullable Integer id, String surveyId, String url, BaseMapEntityType type) {
    return builder().setId(id).setSurveyId(surveyId).setUrl(url).setType(type).build();
  }

  public static Builder builder() {
    return new AutoValue_BaseMapEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(@Nullable Integer newId);

    public abstract Builder setSurveyId(@NonNull String newSurveyId);

    public abstract Builder setUrl(@NonNull String newUrl);

    public abstract Builder setType(BaseMapEntityType type);

    public abstract BaseMapEntity build();
  }
}
