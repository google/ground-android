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
import com.google.android.ground.model.task.MultipleChoice;
import com.google.android.ground.model.task.MultipleChoice.Cardinality;
import com.google.android.ground.persistence.local.room.IntEnum;

/** Defines how Room represents cardinality types in the local db. */
public enum MultipleChoiceEntityType implements IntEnum {
  UNKNOWN(0),
  SELECT_ONE(1),
  SELECT_MULTIPLE(2);

  private final int intValue;

  MultipleChoiceEntityType(int intValue) {
    this.intValue = intValue;
  }

  @Override
  public int intValue() {
    return intValue;
  }

  public static MultipleChoiceEntityType fromCardinality(MultipleChoice.Cardinality type) {
    switch (type) {
      case SELECT_ONE:
        return SELECT_ONE;
      case SELECT_MULTIPLE:
        return SELECT_MULTIPLE;
      default:
        return UNKNOWN;
    }
  }

  public MultipleChoice.Cardinality toCardinality() {
    switch (this) {
      case SELECT_ONE:
        return Cardinality.SELECT_ONE;
      case SELECT_MULTIPLE:
        return Cardinality.SELECT_MULTIPLE;
      default:
        throw new IllegalArgumentException("Unknown cardinality");
    }
  }

  @TypeConverter
  public static int toInt(@Nullable MultipleChoiceEntityType value) {
    return IntEnum.toInt(value, UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static MultipleChoiceEntityType fromInt(int intValue) {
    return IntEnum.fromInt(values(), intValue, UNKNOWN);
  }
}
