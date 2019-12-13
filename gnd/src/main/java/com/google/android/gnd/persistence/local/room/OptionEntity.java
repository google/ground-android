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

package com.google.android.gnd.persistence.local.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.form.Option;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(tableName = "option")
public abstract class OptionEntity {

  @PrimaryKey(autoGenerate = true)
  public int id;

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
  @ColumnInfo(name = "field_id")
  public abstract String getFieldId();

  public static OptionEntity fromOption(String fieldId, Option option) {
    return OptionEntity.builder()
        .setFieldId(fieldId)
        .setCode(option.getCode())
        .setLabel(option.getLabel())
        .build();
  }

  public static OptionEntity create(String code, String label, String fieldId) {
    return builder().setCode(code).setLabel(label).setFieldId(fieldId).build();
  }

  public static Builder builder() {
    return new AutoValue_OptionEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setCode(String code);

    public abstract Builder setLabel(String label);

    public abstract Builder setFieldId(String fieldId);

    public abstract OptionEntity build();
  }
}
