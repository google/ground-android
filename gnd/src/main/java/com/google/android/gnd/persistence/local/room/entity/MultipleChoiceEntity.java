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
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.form.MultipleChoice;
import com.google.android.gnd.model.form.Option;
import com.google.android.gnd.persistence.local.room.models.MultipleChoiceEntityType;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableList;
import java.util.List;

@AutoValue
@Entity(
    tableName = "multiple_choice",
    foreignKeys =
        @ForeignKey(
            entity = FieldEntity.class,
            parentColumns = "id",
            childColumns = "field_id",
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("field_id")})
public abstract class MultipleChoiceEntity {

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "type")
  public abstract MultipleChoiceEntityType getType();

  @CopyAnnotations
  @PrimaryKey
  @NonNull
  @ColumnInfo(name = "field_id")
  public abstract String getFieldId();

  public static MultipleChoiceEntity fromMultipleChoice(
      String fieldId, MultipleChoice multipleChoice) {
    return MultipleChoiceEntity.builder()
        .setFieldId(fieldId)
        .setType(MultipleChoiceEntityType.fromCardinality(multipleChoice.getCardinality()))
        .build();
  }

  static MultipleChoice toMultipleChoice(
      MultipleChoiceEntity multipleChoiceEntity, List<OptionEntity> optionEntities) {
    MultipleChoice.Builder multipleChoiceBuilder =
        MultipleChoice.newBuilder().setCardinality(multipleChoiceEntity.getType().toCardinality());

    ImmutableList.Builder<Option> listBuilder = ImmutableList.builder();
    for (OptionEntity optionEntity : optionEntities) {
      listBuilder.add(OptionEntity.toOption(optionEntity));
    }

    return multipleChoiceBuilder.setOptions(listBuilder.build()).build();
  }

  public static MultipleChoiceEntity create(MultipleChoiceEntityType type, String fieldId) {
    return builder().setType(type).setFieldId(fieldId).build();
  }

  public static Builder builder() {
    return new AutoValue_MultipleChoiceEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setType(MultipleChoiceEntityType type);

    public abstract Builder setFieldId(String fieldId);

    public abstract MultipleChoiceEntity build();
  }
}
