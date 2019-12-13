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
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(
    tableName = "field",
    foreignKeys =
        @ForeignKey(
            entity = FormEntity.class,
            parentColumns = "id",
            childColumns = "form_id",
            onDelete = ForeignKey.CASCADE))
public abstract class FieldEntity {

  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "type")
  public abstract Type getType();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "label")
  public abstract String getLabel();

  @CopyAnnotations
  @ColumnInfo(name = "is_required")
  public abstract boolean isRequired();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "form_id")
  public abstract String getFormId();

  public static FieldEntity fromField(String formId, Field field) {
    return FieldEntity.builder()
        .setId(field.getId())
        .setLabel(field.getLabel())
        .setRequired(field.isRequired())
        .setType(field.getType())
        .setFormId(formId)
        .build();
  }

  public static FieldEntity create(
      String id, Type type, String label, boolean required, String formId) {
    return builder()
        .setId(id)
        .setType(type)
        .setLabel(label)
        .setRequired(required)
        .setFormId(formId)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_FieldEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setType(Type type);

    public abstract Builder setLabel(String label);

    public abstract Builder setRequired(boolean required);

    public abstract Builder setFormId(String formId);

    public abstract FieldEntity build();
  }
}
