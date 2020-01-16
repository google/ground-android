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

package com.google.android.ground.persistence.local.room;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import com.google.android.ground.model.form.Element;
import com.google.android.ground.model.form.Element.Type;

/** Defines how Room represents element types in the local db. */
public enum ElementEntityType implements IntEnum {
  UNKNOWN(0),
  FIELD(1);

  private final int intValue;

  ElementEntityType(int intValue) {
    this.intValue = intValue;
  }

  @Override
  public int intValue() {
    return intValue;
  }

  static ElementEntityType fromElementType(Element.Type type) {
    if (type == Type.FIELD) {
      return FIELD;
    }
    return UNKNOWN;
  }

  Element.Type toElementType() {
    if (this == ElementEntityType.FIELD) {
      return Type.FIELD;
    }
    return Type.UNKNOWN;
  }

  @TypeConverter
  public static int toInt(@Nullable ElementEntityType value) {
    return IntEnum.toInt(value, UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static ElementEntityType fromInt(int intValue) {
    return IntEnum.fromInt(values(), intValue, UNKNOWN);
  }
}
