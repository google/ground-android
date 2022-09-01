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

import static kotlinx.collections.immutable.ExtensionsKt.toPersistentList;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.ground.model.task.MultipleChoice;
import com.google.android.ground.model.task.Option;
import com.google.android.ground.persistence.local.room.models.MultipleChoiceEntityType;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableList;
import java.util.List;

@AutoValue
@Entity(
    tableName = "multiple_choice",
    foreignKeys =
        @ForeignKey(
            entity = TaskEntity.class,
            parentColumns = "id",
            childColumns = "task_id",
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("task_id")})
public abstract class MultipleChoiceEntity {

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "type")
  public abstract MultipleChoiceEntityType getType();

  @CopyAnnotations
  @PrimaryKey
  @NonNull
  @ColumnInfo(name = "task_id")
  public abstract String getTaskId();

  public static MultipleChoiceEntity fromMultipleChoice(
      String taskId, MultipleChoice multipleChoice) {
    return MultipleChoiceEntity.builder()
        .setTaskId(taskId)
        .setType(MultipleChoiceEntityType.fromCardinality(multipleChoice.getCardinality()))
        .build();
  }

  static MultipleChoice toMultipleChoice(
      MultipleChoiceEntity multipleChoiceEntity, List<OptionEntity> optionEntities) {
    ImmutableList.Builder<Option> listBuilder = ImmutableList.builder();
    for (OptionEntity optionEntity : optionEntities) {
      listBuilder.add(OptionEntity.toOption(optionEntity));
    }

    return new MultipleChoice(
        toPersistentList(listBuilder.build()), multipleChoiceEntity.getType().toCardinality());
  }

  public static MultipleChoiceEntity create(MultipleChoiceEntityType type, String taskId) {
    return builder().setType(type).setTaskId(taskId).build();
  }

  public static Builder builder() {
    return new AutoValue_MultipleChoiceEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setType(MultipleChoiceEntityType type);

    public abstract Builder setTaskId(String taskId);

    public abstract MultipleChoiceEntity build();
  }
}
