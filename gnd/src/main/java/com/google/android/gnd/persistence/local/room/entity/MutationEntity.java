/*
 * Copyright 2020 Google LLC
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

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import com.google.android.gnd.persistence.local.room.models.MutationEntityType;
import com.google.auto.value.AutoValue.CopyAnnotations;

/** Base class for feature and observation mutation database entities. */
public abstract class MutationEntity {
  @CopyAnnotations
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  @Nullable
  public abstract Long getId();

  @CopyAnnotations
  @ColumnInfo(name = "project_id")
  public abstract String getProjectId();

  @CopyAnnotations
  @ColumnInfo(name = "type")
  public abstract MutationEntityType getType();

  @CopyAnnotations
  @ColumnInfo(name = "retry_count")
  public abstract long getRetryCount();

  @CopyAnnotations
  @ColumnInfo(name = "last_error")
  @Nullable
  public abstract String getLastError();

  @CopyAnnotations
  @ColumnInfo(name = "user_id")
  public abstract String getUserId();

  @CopyAnnotations
  @ColumnInfo(name = "client_timestamp")
  public abstract long getClientTimestamp();

  public abstract static class Builder<T extends Builder> {

    public abstract T setId(@Nullable Long newId);

    public abstract T setProjectId(String newProjectId);

    public abstract T setType(MutationEntityType newType);

    public abstract T setRetryCount(long newRetryCount);

    public abstract T setLastError(@Nullable String newLastError);

    public abstract T setUserId(String newUserId);

    public abstract T setClientTimestamp(long newClientTimestamp);
  }
}
