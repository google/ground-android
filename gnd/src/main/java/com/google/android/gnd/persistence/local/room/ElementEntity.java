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
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Element.Type;
import com.google.android.gnd.model.form.Field;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(
    tableName = "element",
    foreignKeys =
        @ForeignKey(
            entity = FormEntity.class,
            parentColumns = "id",
            childColumns = "form_id",
            onDelete = ForeignKey.CASCADE))
public abstract class ElementEntity {

  @PrimaryKey(autoGenerate = true)
  public int id;

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "form_id")
  public abstract String getFormId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "type")
  public abstract Type getType();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "field_id")
  public abstract String getFieldId();

  public static ElementEntity fromElement(String formId, Element element) {
    String fieldId = element.getField() != null ? element.getField().getId() : null;
    return ElementEntity.builder()
        .setFormId(formId)
        .setType(element.getType())
        .setFieldId(fieldId)
        .build();
  }

  public static Element toElement(ElementEntity elementEntity, FieldData fieldData) {
    if (elementEntity.getType() == Type.FIELD) {
      Field field = FieldEntity.toField(fieldData);
      return Element.ofField(field);
    } else {
      return Element.ofUnknown();
    }
  }

  public static ElementEntity create(String formId, Type type, String fieldId) {
    return builder().setFormId(formId).setType(type).setFieldId(fieldId).build();
  }

  public static Builder builder() {
    return new AutoValue_ElementEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setFormId(String formId);

    public abstract Builder setType(Type type);

    public abstract Builder setFieldId(String fieldId);

    public abstract ElementEntity build();
  }
}
