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
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.persistence.local.room.relations.JobEntityAndRelations;
import com.google.android.ground.persistence.local.room.relations.TaskEntityAndRelations;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableMap;

@AutoValue
@Entity(
    tableName = "job",
    foreignKeys =
    @ForeignKey(
        entity = SurveyEntity.class,
        parentColumns = "id",
        childColumns = "survey_id",
        onDelete = ForeignKey.CASCADE),
    indices = {@Index("survey_id")})
public abstract class JobEntity {

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

  public static JobEntity fromJob(String surveyId, Job job) {
    return JobEntity.builder()
        .setId(job.getId())
        .setSurveyId(surveyId)
        .setName(job.getName())
        .build();
  }

  static Job toJob(JobEntityAndRelations jobEntityAndRelations) {
    JobEntity jobEntity = jobEntityAndRelations.jobEntity;

    ImmutableMap.Builder<String, Task> taskMap = ImmutableMap.builder();
    for (TaskEntityAndRelations taskEntityAndRelations :
        jobEntityAndRelations.taskEntityAndRelations) {
      Task task = TaskEntity.toTask(taskEntityAndRelations);
      taskMap.put(task.getId(), task);
    }

    return new Job(jobEntity.getId(), jobEntity.getName(), taskMap.build());
  }

  public static JobEntity create(String id, String name, String surveyId) {
    return builder()
        .setId(id)
        .setName(name)
        .setSurveyId(surveyId)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_JobEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setName(String name);

    public abstract Builder setSurveyId(String surveyId);

    public abstract JobEntity build();
  }
}
