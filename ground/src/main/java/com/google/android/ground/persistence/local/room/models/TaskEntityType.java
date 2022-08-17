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

package com.google.android.ground.persistence.local.room.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import com.google.android.ground.model.task.Task.Type;
import com.google.android.ground.persistence.local.room.IntEnum;
import com.google.common.collect.ImmutableBiMap;

public enum TaskEntityType implements IntEnum {
  UNKNOWN(0),
  TEXT(1),
  MULTIPLE_CHOICE(2),
  PHOTO(3),
  NUMBER(4),
  DATE(5),
  TIME(6);

  private final int intValue;

  private static final ImmutableBiMap<TaskEntityType, Type> TASK_TYPES =
      ImmutableBiMap.<TaskEntityType, Type>builder()
          .put(TEXT, Type.TEXT)
          .put(MULTIPLE_CHOICE, Type.MULTIPLE_CHOICE)
          .put(PHOTO, Type.PHOTO)
          .put(NUMBER, Type.NUMBER)
          .put(DATE, Type.DATE)
          .put(TIME, Type.TIME)
          .build();

  TaskEntityType(int intValue) {
    this.intValue = intValue;
  }

  @Override
  public int intValue() {
    return intValue;
  }

  public static TaskEntityType fromTaskType(Type type) {
    return TASK_TYPES.inverse().getOrDefault(type, TaskEntityType.UNKNOWN);
  }

  public Type toTaskType() {
    return TASK_TYPES.getOrDefault(this, Type.UNKNOWN);
  }

  @TypeConverter
  public static int toInt(@Nullable TaskEntityType value) {
    return IntEnum.toInt(value, UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static TaskEntityType fromInt(int intValue) {
    return IntEnum.fromInt(values(), intValue, UNKNOWN);
  }
}
