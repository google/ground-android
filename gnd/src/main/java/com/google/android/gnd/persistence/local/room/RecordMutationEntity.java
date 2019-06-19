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
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.persistence.shared.RecordMutation;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import org.json.JSONObject;

/**
 * Representation of a {@link com.google.android.gnd.persistence.shared.RecordMutation} in local
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
  @Nullable
  public abstract Long getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "record_id")
  public abstract String getRecordId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "type")
  public abstract MutationEntityType getType();

  /**
   * For mutations of type {@link MutationEntityType#CREATE} and {@link MutationEntityType#UPDATE},
   * returns a {@link JSONObject} with the new values of modified form responses, with {@code null}
   * values representing responses that were removed/cleared.
   *
   * <p>This method returns {@code null} for mutation type {@link MutationEntityType#DELETE}.
   */
  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "new_responses")
  public abstract JSONObject getModifiedResponses();

  public static RecordMutationEntity create(
      long id, String recordId, MutationEntityType type, JSONObject modifiedResponses) {
    return builder()
        .setId(id)
        .setRecordId(recordId)
        .setType(type)
        .setModifiedResponses(modifiedResponses)
        .build();
  }

  public static RecordMutationEntity fromMutation(RecordMutation mutation) {
    return RecordMutationEntity.builder()
        .setRecordId(mutation.getRecordId())
        .setType(MutationEntityType.fromMutationType(mutation.getType()))
        .setModifiedResponses(RecordEntity.convertResponsesToJson(mutation.getModifiedResponses()))
        .build();
  }

  // Auto-generated boilerplate.

  public static Builder builder() {
    return new AutoValue_RecordMutationEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(@Nullable Long newId);

    public abstract Builder setRecordId(String newRecordId);

    public abstract Builder setType(MutationEntityType newType);

    public abstract Builder setModifiedResponses(JSONObject newModifiedResponses);

    public abstract RecordMutationEntity build();
  }
}
