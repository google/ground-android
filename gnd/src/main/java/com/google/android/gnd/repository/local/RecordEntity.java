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
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.vo.Record.Value;
import java.util.Map;

/** Representation of a {@link com.google.android.gnd.vo.Record} in local db. */
// TODO: Convert to AutoValue class.
@Entity(
    tableName = "record",
    indices = {@Index("id")})
public class RecordEntity {
  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  public String id;

  @ColumnInfo(name = "state")
  @NonNull
  public EntityState state;

  // Add Converter to convert to/from JSON.
  @Ignore
  @ColumnInfo(name = "values")
  @NonNull
  public Map<String, Value> values;
}
