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

package com.google.android.ground.persistence.local.room.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.ground.model.task.MultipleChoice;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.persistence.local.room.models.TaskEntityType;
import com.google.android.ground.persistence.local.room.relations.TaskEntityAndRelations;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import java.util.List;
import timber.log.Timber;

@AutoValue
@Entity(
    tableName = "task",
    foreignKeys =
        @ForeignKey(
            entity = JobEntity.class,
            parentColumns = "id",
            childColumns = "job_id",
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("job_id")})
public abstract class TaskEntity {

  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @ColumnInfo(name = "index")
  public abstract int getIndex();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "task_type")
  public abstract TaskEntityType getTaskType();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "label")
  public abstract String getLabel();

  @CopyAnnotations
  @ColumnInfo(name = "is_required")
  public abstract boolean isRequired();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "job_id")
  public abstract String getJobId();

  public static TaskEntity fromTask(String jobId, Task task) {
    return TaskEntity.builder()
        .setId(task.getId())
        .setJobId(jobId)
        .setIndex(task.getIndex())
        .setLabel(task.getLabel())
        .setRequired(task.isRequired())
        .setTaskType(TaskEntityType.fromTaskType(task.getType()))
        .build();
  }

  static Task toTask(TaskEntityAndRelations taskEntityAndRelations) {
    TaskEntity taskEntity = taskEntityAndRelations.taskEntity;

    List<MultipleChoiceEntity> multipleChoiceEntities =
        taskEntityAndRelations.multipleChoiceEntities;

    MultipleChoice multipleChoice = null;
    if (!multipleChoiceEntities.isEmpty()) {
      if (multipleChoiceEntities.size() > 1) {
        Timber.e("More than 1 multiple choice found for task");
      }

      multipleChoice =
          MultipleChoiceEntity.toMultipleChoice(
              multipleChoiceEntities.get(0), taskEntityAndRelations.optionEntities);
    }

    return new Task(
        taskEntity.getId(),
        taskEntity.getIndex(),
        taskEntity.getTaskType().toTaskType(),
        taskEntity.getLabel(),
        taskEntity.isRequired(),
        multipleChoice);
  }

  public static TaskEntity create(
      String id,
      Integer index,
      TaskEntityType taskType,
      String label,
      boolean required,
      String jobId) {
    return builder()
        .setId(id)
        .setIndex(index)
        .setTaskType(taskType)
        .setLabel(label)
        .setRequired(required)
        .setJobId(jobId)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_TaskEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setIndex(int id);

    public abstract Builder setTaskType(TaskEntityType taskType);

    public abstract Builder setLabel(String label);

    public abstract Builder setRequired(boolean required);

    public abstract Builder setJobId(String jobId);

    public abstract TaskEntity build();
  }
}
