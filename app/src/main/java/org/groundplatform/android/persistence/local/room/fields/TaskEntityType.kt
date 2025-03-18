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
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.persistence.local.room.IntEnum

enum class TaskEntityType(private val intValue: Int) : IntEnum {
  UNKNOWN(0),
  TEXT(1),
  MULTIPLE_CHOICE(2),
  PHOTO(3),
  NUMBER(4),
  DATE(5),
  TIME(6),
  POINT(7),
  POLYGON(8),
  CAPTURE_LOCATION(9),
  INSTRUCTIONS(10);

  override fun intValue(): Int = intValue

  fun toTaskType(): Task.Type = TASK_TYPES.getOrDefault(this, Task.Type.UNKNOWN)

  companion object {
    private val TASK_TYPES: Map<TaskEntityType, Task.Type> =
      mapOf(
        Pair(TEXT, Task.Type.TEXT),
        Pair(MULTIPLE_CHOICE, Task.Type.MULTIPLE_CHOICE),
        Pair(PHOTO, Task.Type.PHOTO),
        Pair(NUMBER, Task.Type.NUMBER),
        Pair(DATE, Task.Type.DATE),
        Pair(TIME, Task.Type.TIME),
        Pair(POINT, Task.Type.DROP_PIN),
        Pair(POLYGON, Task.Type.DRAW_AREA),
        Pair(CAPTURE_LOCATION, Task.Type.CAPTURE_LOCATION),
        Pair(INSTRUCTIONS, Task.Type.INSTRUCTIONS),
      )
    private val REVERSE_TASK_TYPES: Map<Task.Type, TaskEntityType> =
      TASK_TYPES.entries.associateBy({ it.value }) { it.key }

    fun fromTaskType(type: Task.Type): TaskEntityType =
      REVERSE_TASK_TYPES.getOrDefault(type, UNKNOWN)

    @JvmStatic @TypeConverter fun toInt(value: TaskEntityType?): Int = IntEnum.toInt(value, UNKNOWN)

    @JvmStatic
    @TypeConverter
    fun fromInt(intValue: Int): TaskEntityType =
      IntEnum.fromInt(entries.toTypedArray(), intValue, UNKNOWN)
  }
}
