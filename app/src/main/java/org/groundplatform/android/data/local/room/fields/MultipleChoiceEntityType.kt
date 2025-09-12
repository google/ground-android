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
package org.groundplatform.android.data.local.room.fields

import androidx.room.TypeConverter
import org.groundplatform.android.data.local.room.IntEnum
import org.groundplatform.android.data.local.room.IntEnum.Companion.fromInt
import org.groundplatform.android.data.local.room.IntEnum.Companion.toInt
import org.groundplatform.android.model.task.MultipleChoice

/** Defines how Room represents cardinality types in the local db. */
enum class MultipleChoiceEntityType(private val intValue: Int) : IntEnum {
  UNKNOWN(0),
  SELECT_ONE(1),
  SELECT_MULTIPLE(2);

  override fun intValue() = intValue

  fun toCardinality() =
    when (this) {
      SELECT_ONE -> MultipleChoice.Cardinality.SELECT_ONE
      SELECT_MULTIPLE -> MultipleChoice.Cardinality.SELECT_MULTIPLE
      else -> throw IllegalArgumentException("Unknown cardinality")
    }

  companion object {
    fun fromCardinality(type: MultipleChoice.Cardinality?) =
      when (type) {
        MultipleChoice.Cardinality.SELECT_ONE -> SELECT_ONE
        MultipleChoice.Cardinality.SELECT_MULTIPLE -> SELECT_MULTIPLE
        else -> UNKNOWN
      }

    @JvmStatic @TypeConverter fun toInt(value: MultipleChoiceEntityType?) = toInt(value, UNKNOWN)

    @JvmStatic
    @TypeConverter
    fun fromInt(intValue: Int) = fromInt(entries.toTypedArray(), intValue, UNKNOWN)
  }
}
