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
import com.google.android.gnd.model.layer.Job;
import com.google.android.gnd.persistence.local.room.relations.FormEntityAndRelations;
import com.google.android.gnd.persistence.local.room.relations.LayerEntityAndRelations;
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

  public static LayerEntity fromLayer(String surveyId, Job job) {
    return LayerEntity.builder()
        .setId(job.getId())
        .setSurveyId(surveyId)
        .setName(job.getName())
        .build();
  }

  static Job toLayer(LayerEntityAndRelations layerEntityAndRelations) {
    LayerEntity layerEntity = layerEntityAndRelations.layerEntity;
    Job.Builder layerBuilder =
        Job.newBuilder()
            .setId(layerEntity.getId())
            .setName(layerEntity.getName());

    for (FormEntityAndRelations formEntityAndRelations :
        layerEntityAndRelations.formEntityAndRelations) {
      layerBuilder.setForm(FormEntity.toForm(formEntityAndRelations));
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
