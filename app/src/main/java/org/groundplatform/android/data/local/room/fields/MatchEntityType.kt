/*
 * Copyright 2024 Google LLC
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
import org.groundplatform.android.model.task.Condition

enum class MatchEntityType(private val intValue: Int) : IntEnum {
  UNKNOWN(0),
  MATCH_ANY(1),
  MATCH_ALL(2),
  MATCH_ONE(3);

  override fun intValue(): Int = intValue

  fun toMatchType(): Condition.MatchType =
    MATCH_TYPES.getOrDefault(this, Condition.MatchType.UNKNOWN)

  companion object {
    private val MATCH_TYPES: Map<MatchEntityType, Condition.MatchType> =
      mapOf(
        Pair(UNKNOWN, Condition.MatchType.UNKNOWN),
        Pair(MATCH_ANY, Condition.MatchType.MATCH_ANY),
        Pair(MATCH_ALL, Condition.MatchType.MATCH_ALL),
        Pair(MATCH_ONE, Condition.MatchType.MATCH_ONE),
      )
    private val REVERSE_MATCH_TYPES: Map<Condition.MatchType, MatchEntityType> =
      MATCH_TYPES.entries.associateBy({ it.value }) { it.key }

    fun fromMatchType(type: Condition.MatchType): MatchEntityType =
      REVERSE_MATCH_TYPES.getOrDefault(type, UNKNOWN)

    @JvmStatic
    @TypeConverter
    fun toInt(value: MatchEntityType?): Int = IntEnum.toInt(value, UNKNOWN)

    @JvmStatic
    @TypeConverter
    fun fromInt(intValue: Int): MatchEntityType =
      IntEnum.fromInt(entries.toTypedArray(), intValue, UNKNOWN)
  }
}
