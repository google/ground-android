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
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.local.room.relations.FormEntityAndRelations;
import com.google.android.gnd.persistence.local.room.relations.LayerEntityAndRelations;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableList;
import org.json.JSONArray;

@AutoValue
@Entity(
    tableName = "layer",
    foreignKeys =
        @ForeignKey(
            entity = SurveyEntity.class,
            parentColumns = "id",
            childColumns = "survey_id",
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("survey_id")})
public abstract class LayerEntity {

  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "name")
  public abstract String getName();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "survey_id")
  public abstract String getSurveyId();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "contributors_can_add")
  public abstract JSONArray getContributorsCanAdd();

  public static LayerEntity fromLayer(String surveyId, Layer layer) {
    return LayerEntity.builder()
        .setId(layer.getId())
        .setSurveyId(surveyId)
        .setName(layer.getName())
        .setContributorsCanAdd(new JSONArray(layer.getContributorsCanAdd()))
        .build();
  }

  static Layer toLayer(LayerEntityAndRelations layerEntityAndRelations) {
    LayerEntity layerEntity = layerEntityAndRelations.layerEntity;
    Layer.Builder layerBuilder =
        Layer.newBuilder()
            .setId(layerEntity.getId())
            .setName(layerEntity.getName());

    for (FormEntityAndRelations formEntityAndRelations :
        layerEntityAndRelations.formEntityAndRelations) {
      layerBuilder.setForm(FormEntity.toForm(formEntityAndRelations));
    }

    layerBuilder.setContributorsCanAdd(toFeatureTypes(layerEntity.getContributorsCanAdd()));

    return layerBuilder.build();
  }

  private static ImmutableList<FeatureType> toFeatureTypes(JSONArray jsonArray) {
    ImmutableList.Builder<FeatureType> builder = ImmutableList.builder();
    for (int i = 0; i < jsonArray.length(); i++) {
      String value = jsonArray.optString(i, null);
      if (value != null) {
        builder.add(toFeatureType(value));
      }
    }
    return builder.build();
  }

  private static FeatureType toFeatureType(String stringValue) {
    switch (stringValue) {
      case "points":
        return FeatureType.POINT;
      case "polygons":
        return FeatureType.POLYGON;
      default:
        return FeatureType.UNKNOWN;
    }
  }

  public static LayerEntity create(
      String id, String name, String surveyId, JSONArray contributorsCanAdd) {
    return builder()
        .setId(id)
        .setName(name)
        .setSurveyId(surveyId)
        .setContributorsCanAdd(contributorsCanAdd)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_LayerEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setName(String name);

    public abstract Builder setSurveyId(String surveyId);

    public abstract Builder setContributorsCanAdd(JSONArray contributorsCanAdd);

    public abstract LayerEntity build();
  }
}
