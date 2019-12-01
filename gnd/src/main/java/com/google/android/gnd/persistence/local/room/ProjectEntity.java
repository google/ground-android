package com.google.android.gnd.persistence.local.room;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.Project;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

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
  @NonNull
  @ColumnInfo(name = "state")
  public abstract EntityState getState();

  public static ProjectEntity fromProject(Project project) {
    return ProjectEntity.builder()
        .setId(project.getId())
        .setTitle(project.getTitle())
        .setDescription(project.getDescription())
        .setState(EntityState.DEFAULT)
        .build();
  }

  public static Project toProject(ProjectEntity entity) {
    return Project.newBuilder()
        .setId(entity.getId())
        .setTitle(entity.getTitle())
        .setDescription(entity.getDescription())
        .build();
  }

  public static ProjectEntity create(
      String id, String title, String description, EntityState state) {
    return builder().setId(id).setTitle(title).setDescription(description).setState(state).build();
  }

  public static Builder builder() {
    return new AutoValue_ProjectEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setTitle(String title);

    public abstract Builder setDescription(String description);

    public abstract Builder setState(EntityState newState);

    public abstract ProjectEntity build();
  }
}
