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

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/**
 * Representation of a {@link com.google.android.gnd.persistence.shared.FeatureMutation} in local
 * data store.
 */
@AutoValue
@Entity(
    tableName = "record_mutation",
    foreignKeys =
        @ForeignKey(
            entity = RecordEntity.class,
            parentColumns = "id",
            childColumns = "record_id",
            onDelete = CASCADE),
    indices = {@Index("record_id")})
public abstract class RecordMutationEntity {
  @CopyAnnotations
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  public abstract int getId();

  @CopyAnnotations
  @ColumnInfo(name = "record_id")
  @NonNull
  public abstract String getRecordId();

  @CopyAnnotations
  @Embedded
  @NonNull
  public abstract Edit getEdit();

  // Auto-generated boilerplate:

  public static RecordMutationEntity create(int id, String recordId, Edit edit) {
    return builder().setId(id).setRecordId(recordId).setEdit(edit).build();
  }

  public static Builder builder() {
    return new AutoValue_RecordMutationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(int newId);

    public abstract Builder setRecordId(String newRecordId);

    public abstract Builder setEdit(Edit newEdit);

    public abstract RecordMutationEntity build();
  }
}
