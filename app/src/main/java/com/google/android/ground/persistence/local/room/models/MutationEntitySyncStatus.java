/*
 * Copyright 2021 Google LLC
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

package com.google.android.ground.persistence.local.room.models;

import static java8.util.J8Arrays.stream;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import com.google.android.ground.model.mutation.Mutation.SyncStatus;
import com.google.android.ground.persistence.local.room.IntEnum;

/** Mutually exclusive mutations states. */
public enum MutationEntitySyncStatus implements IntEnum {
  // TODO(#950): Set IN_PROGRESS and FAILED statuses when necessary. On failure, set retry count and
  // error and update to PENDING.
  UNKNOWN(0, SyncStatus.UNKNOWN),
  /** Pending includes failed sync attempts pending retry. */
  PENDING(1, SyncStatus.PENDING),
  IN_PROGRESS(2, SyncStatus.IN_PROGRESS),
  COMPLETED(3, SyncStatus.COMPLETED),
  /** Failed indicates all retries have failed. */
  FAILED(4, SyncStatus.FAILED);

  private final int intValue;
  private final SyncStatus enumValue;

  MutationEntitySyncStatus(int intValue, SyncStatus enumValue) {
    this.intValue = intValue;
    this.enumValue = enumValue;
  }

  public static MutationEntitySyncStatus fromMutationSyncStatus(SyncStatus syncStatus) {
    return stream(MutationEntitySyncStatus.values())
        .filter(s -> s.enumValue == syncStatus)
        .findFirst()
        .orElse(MutationEntitySyncStatus.UNKNOWN);
  }

  public int intValue() {
    return intValue;
  }

  public SyncStatus toMutationSyncStatus() {
    return enumValue;
  }

  @TypeConverter
  public static int toInt(@Nullable MutationEntitySyncStatus value) {
    return IntEnum.toInt(value, UNKNOWN);
  }

  @TypeConverter
  public static MutationEntitySyncStatus fromInt(int intValue) {
    return IntEnum.fromInt(values(), intValue, UNKNOWN);
  }
}
