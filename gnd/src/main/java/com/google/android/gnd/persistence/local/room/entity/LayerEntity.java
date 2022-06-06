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
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.local.room.relations.LayerEntityAndRelations;
import com.google.android.gnd.persistence.local.room.relations.TaskEntityAndRelations;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

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

  public static LayerEntity fromLayer(String surveyId, Layer layer) {
    return LayerEntity.builder()
        .setId(layer.getId())
        .setSurveyId(surveyId)
        .setName(layer.getName())
        .build();
  }

  static Layer toLayer(LayerEntityAndRelations layerEntityAndRelations) {
    LayerEntity layerEntity = layerEntityAndRelations.layerEntity;
    Layer.Builder layerBuilder =
        Layer.newBuilder()
            .setId(layerEntity.getId())
            .setName(layerEntity.getName());

    for (TaskEntityAndRelations taskEntityAndRelations :
        layerEntityAndRelations.taskEntityAndRelations) {
      layerBuilder.setTask(TaskEntity.toTask(taskEntityAndRelations));
    }

    return layerBuilder.build();
  }

  public static LayerEntity create(String id, String name, String surveyId) {
    return builder()
        .setId(id)
        .setName(name)
        .setSurveyId(surveyId)
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

    public abstract LayerEntity build();
  }
}
