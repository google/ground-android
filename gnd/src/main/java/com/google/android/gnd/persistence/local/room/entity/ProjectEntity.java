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
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.persistence.local.room.relations.LayerEntityAndRelations;
import com.google.android.gnd.persistence.local.room.relations.ProjectEntityAndRelations;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableMap;
import java.net.MalformedURLException;
import java.util.Iterator;
import org.json.JSONObject;
import timber.log.Timber;

@AutoValue
@Entity(tableName = "project")
public abstract class ProjectEntity {

  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "title")
  public abstract String getTitle();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "description")
  public abstract String getDescription();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "acl")
  public abstract JSONObject getAcl();

  public static ProjectEntity fromProject(Project project) {
    return ProjectEntity.builder()
        .setId(project.getId())
        .setTitle(project.getTitle())
        .setDescription(project.getDescription())
        .setAcl(new JSONObject(project.getAcl()))
        .build();
  }

  public static Project toProject(ProjectEntityAndRelations projectEntityAndRelations) {
    ProjectEntity projectEntity = projectEntityAndRelations.projectEntity;
    Project.Builder projectBuilder =
        Project.newBuilder()
            .setId(projectEntity.getId())
            .setTitle(projectEntity.getTitle())
            .setDescription(projectEntity.getDescription())
            .setAcl(toStringMap(projectEntity.getAcl()));

    for (LayerEntityAndRelations layerEntityAndRelations :
        projectEntityAndRelations.layerEntityAndRelations) {
      LayerEntity layerEntity = layerEntityAndRelations.layerEntity;
      projectBuilder.putLayer(layerEntity.getId(), LayerEntity.toLayer(layerEntityAndRelations));
    }
    for (OfflineBaseMapSourceEntity source :
        projectEntityAndRelations.offlineBaseMapSourceEntityAndRelations) {
      try {
        projectBuilder.addOfflineBaseMapSource(OfflineBaseMapSourceEntity.toModel(source));
      } catch (MalformedURLException e) {
        Timber.d("Skipping basemap source with malformed URL %s", source.getUrl());
      }
    }

    return projectBuilder.build();
  }

  private static ImmutableMap<String, String> toStringMap(JSONObject jsonObject) {
    ImmutableMap.Builder builder = ImmutableMap.builder();
    Iterator<String> keys = jsonObject.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      String value = jsonObject.optString(key, null);
      if (value != null) {
        builder.put(key, value);
      }
    }
    return builder.build();
  }

  public static ProjectEntity create(String id, String title, String description, JSONObject acl) {
    return builder().setId(id).setTitle(title).setDescription(description).setAcl(acl).build();
  }

  public static Builder builder() {
    return new AutoValue_ProjectEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setTitle(String title);

    public abstract Builder setDescription(String description);

    public abstract Builder setAcl(JSONObject acl);

    public abstract ProjectEntity build();
  }
}
