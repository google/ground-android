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

package com.google.android.gnd.persistence.local.room;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import com.google.android.gnd.model.form.Field;

public enum FieldEntityType implements IntEnum {
  UNKNOWN(0),
  TEXT(1),
  MULTIPLE_CHOICE(2);

  private final int intValue;

  FieldEntityType(int intValue) {
    this.intValue = intValue;
  }

  @Override
  public int intValue() {
    return intValue;
  }

  static FieldEntityType fromFieldType(Field.Type type) {
    switch (type) {
      case TEXT:
        return TEXT;
      case MULTIPLE_CHOICE:
        return MULTIPLE_CHOICE;
      default:
        return UNKNOWN;
    }
  }

  Field.Type toFieldType() {
    switch (this) {
      case TEXT:
        return Field.Type.TEXT;
      case MULTIPLE_CHOICE:
        return Field.Type.MULTIPLE_CHOICE;
      default:
        throw new IllegalArgumentException("Unknown field type");
    }
  }

  @TypeConverter
  public static int toInt(@Nullable FieldEntityType value) {
    return IntEnum.toInt(value, UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static FieldEntityType fromInt(int intValue) {
    return IntEnum.fromInt(values(), intValue, UNKNOWN);
  }
}
