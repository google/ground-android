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
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import org.json.JSONObject;

/**
 * Representation of a {@link com.google.android.gnd.persistence.shared.RecordMutation} in local
 * data store.
 */
@Entity(
    tableName = "recordMutation",
    foreignKeys =
        @ForeignKey(
            entity = RecordEntity.class,
            parentColumns = "id",
            childColumns = "recordId",
            onDelete = CASCADE),
    indices = {@Index("recordId")})
public class RecordMutationEntity {
  @PrimaryKey(autoGenerate = true)
  public int id;

  @NonNull public String recordId;

  @NonNull public MutationEntityType type;

  /**
   * For edits of type {@link MutationEntityType#CREATE} and {@link MutationEntityType#UPDATE}, a
   * JSON object with the new value of modified attributes after this mutation. For all other
   * mutation types this will be null.
   *
   * <p>Null values in the responses indicates a response was removed/cleared.
   */
  @Nullable public JSONObject newResponses;
}
