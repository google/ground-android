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
package org.groundplatform.android.persistence.local.room.fields

import androidx.room.TypeConverter
import org.groundplatform.android.persistence.local.room.IntEnum
import org.groundplatform.android.persistence.local.room.IntEnum.Companion.fromInt
import org.groundplatform.android.persistence.local.room.IntEnum.Companion.toInt

/** Mutually exclusive entity states shared by LOIs and Submissions. */
enum class EntityDeletionState(private val intValue: Int) : IntEnum {
  UNKNOWN(0),
  DEFAULT(1),
  DELETED(2);

  override fun intValue() = intValue

  companion object {
    @JvmStatic @TypeConverter fun toInt(value: EntityDeletionState?) = toInt(value, UNKNOWN)

    @JvmStatic
    @TypeConverter
    fun fromInt(intValue: Int) = fromInt(entries.toTypedArray(), intValue, UNKNOWN)
  }
}
