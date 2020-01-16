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

package com.google.android.ground.persistence.local.room;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.ground.model.layer.Layer;
import com.google.android.ground.model.layer.Style;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(
    tableName = "layer",
    foreignKeys =
        @ForeignKey(
            entity = ProjectEntity.class,
            parentColumns = "id",
            childColumns = "project_id",
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("project_id")})
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
  @ColumnInfo(name = "default_style")
  public abstract Style getDefaultStyle();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "project_id")
  public abstract String getProjectId();

  public static LayerEntity fromLayer(String projectId, Layer layer) {
    return LayerEntity.builder()
        .setId(layer.getId())
        .setProjectId(projectId)
        .setName(layer.getName())
        .setDefaultStyle(layer.getDefaultStyle())
        .build();
  }

  public static Layer toLayer(LayerEntityAndRelations layerEntityAndRelations) {
    LayerEntity layerEntity = layerEntityAndRelations.layerEntity;
    Layer.Builder layerBuilder =
        Layer.newBuilder()
            .setId(layerEntity.getId())
            .setDefaultStyle(layerEntity.getDefaultStyle())
            .setName(layerEntity.getName());

    for (FormEntityAndRelations formEntityAndRelations :
        layerEntityAndRelations.formEntityAndRelations) {
      layerBuilder.addForm(FormEntity.toForm(formEntityAndRelations));
    }

    return layerBuilder.build();
  }

  public static LayerEntity create(String id, String name, Style defaultStyle, String projectId) {
    return builder()
        .setId(id)
        .setName(name)
        .setDefaultStyle(defaultStyle)
        .setProjectId(projectId)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_LayerEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setName(String name);

    public abstract Builder setDefaultStyle(Style defaultStyle);

    public abstract Builder setProjectId(String projectId);

    public abstract LayerEntity build();
  }
}
