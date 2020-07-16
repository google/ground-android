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
import com.google.android.gnd.model.form.Option;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(
    tableName = "option",
    foreignKeys =
        @ForeignKey(
            entity = FieldEntity.class,
            parentColumns = "id",
            childColumns = "field_id", // NOPMD
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("field_id")}, // NOPMD
    primaryKeys = {"code", "field_id"}) // NOPMD
public abstract class OptionEntity {
  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "code")
  public abstract String getCode();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "label")
  public abstract String getLabel();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "field_id") // NOPMD
  public abstract String getFieldId();

  public static OptionEntity fromOption(String fieldId, Option option) {
    return OptionEntity.builder()
        .setId(option.getId())
        .setFieldId(fieldId)
        .setCode(option.getCode())
        .setLabel(option.getLabel())
        .build();
  }

  public static Option toOption(OptionEntity optionEntity) {
    return Option.newBuilder()
        .setId(optionEntity.getId())
        .setCode(optionEntity.getCode())
        .setLabel(optionEntity.getLabel())
        .build();
  }

  public static OptionEntity create(String id, String code, String label, String fieldId) {
    return builder().setId(id).setCode(code).setLabel(label).setFieldId(fieldId).build();
  }

  public static Builder builder() {
    return new AutoValue_OptionEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(@NonNull String id);

    public abstract Builder setCode(String code);

    public abstract Builder setLabel(String label);

    public abstract Builder setFieldId(String fieldId);

    public abstract OptionEntity build();
  }
}
