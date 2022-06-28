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

package com.google.android.gnd.persistence.local.room.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import com.google.android.gnd.model.task.Step;
import com.google.android.gnd.model.task.Step.Type;
import com.google.android.gnd.persistence.local.room.IntEnum;

/**
 * Defines how Room represents element types in the local db.
 */
public enum StepEntityType implements IntEnum {
  UNKNOWN(0),
  FIELD(1);

  private final int intValue;

  StepEntityType(int intValue) {
    this.intValue = intValue;
  }

  @Override
  public int intValue() {
    return intValue;
  }

  public static StepEntityType fromStepType(Step.Type type) {
    if (type == Type.FIELD) {
      return FIELD;
    }
    return UNKNOWN;
  }

  public Step.Type toStepType() {
    if (this == StepEntityType.FIELD) {
      return Type.FIELD;
    }
    return Type.UNKNOWN;
  }

  @TypeConverter
  public static int toInt(@Nullable StepEntityType value) {
    return IntEnum.toInt(value, UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static StepEntityType fromInt(int intValue) {
    return IntEnum.fromInt(values(), intValue, UNKNOWN);
  }
}
