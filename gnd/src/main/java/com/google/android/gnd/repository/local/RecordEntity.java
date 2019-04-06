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

package com.google.android.gnd.repository.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import org.json.JSONObject;

/** Representation of a {@link com.google.android.gnd.vo.Record} in local db. */
@AutoValue
@Entity(
    tableName = "record",
    indices = {@Index("id")})
public abstract class RecordEntity {
  @CopyAnnotations
  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  public abstract String getId();

  @CopyAnnotations
  @ColumnInfo(name = "state")
  @NonNull
  public abstract EntityState getState();

  @CopyAnnotations
  @ColumnInfo(name = "responses")
  @NonNull
  public abstract JSONObject getResponses();

  // Auto-generated boilerplate:

  public static RecordEntity create(String id, EntityState state, JSONObject responses) {
    return builder().setId(id).setState(state).setResponses(responses).build();
  }

  public static Builder builder() {
    return new AutoValue_RecordEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setResponses(JSONObject newResponses);

    public abstract RecordEntity build();
  }
}
