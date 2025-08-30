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
package org.groundplatform.android.data.local.room.fields

import androidx.room.TypeConverter
import org.groundplatform.android.data.local.room.IntEnum
import org.groundplatform.android.data.local.room.IntEnum.Companion.fromInt
import org.groundplatform.android.data.local.room.IntEnum.Companion.toInt
import org.groundplatform.android.model.mutation.Mutation.SyncStatus

/** Mutually exclusive mutations states. */
enum class MutationEntitySyncStatus(private val intValue: Int, private val enumValue: SyncStatus) :
  IntEnum {
  // TODO: Set IN_PROGRESS and FAILED statuses
  //  when necessary. On failure, set retry count and error and update to PENDING.
  // Issue URL: https://github.com/google/ground-android/issues/950
  UNKNOWN(0, SyncStatus.UNKNOWN),

  /** Pending includes failed sync attempts pending retry. */
  PENDING(1, SyncStatus.PENDING),
  IN_PROGRESS(2, SyncStatus.IN_PROGRESS),
  MEDIA_UPLOAD_PENDING(5, SyncStatus.MEDIA_UPLOAD_PENDING),
  MEDIA_UPLOAD_IN_PROGRESS(6, SyncStatus.MEDIA_UPLOAD_IN_PROGRESS),
  MEDIA_UPLOAD_AWAITING_RETRY(7, SyncStatus.MEDIA_UPLOAD_AWAITING_RETRY),
  COMPLETED(3, SyncStatus.COMPLETED),

  /** Failed indicates all retries have failed. */
  FAILED(4, SyncStatus.FAILED);

  override fun intValue() = intValue

  fun toMutationSyncStatus() = enumValue

  companion object {
    fun fromMutationSyncStatus(syncStatus: SyncStatus) =
      entries.firstOrNull { s: MutationEntitySyncStatus -> s.enumValue === syncStatus } ?: UNKNOWN

    @JvmStatic @TypeConverter fun toInt(value: MutationEntitySyncStatus?) = toInt(value, UNKNOWN)

    @JvmStatic
    @TypeConverter
    fun fromInt(intValue: Int) = fromInt(entries.toTypedArray(), intValue, UNKNOWN)
  }
}
