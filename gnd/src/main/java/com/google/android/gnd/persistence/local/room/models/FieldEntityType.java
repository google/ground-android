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
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.persistence.local.room.IntEnum;
import com.google.common.collect.ImmutableBiMap;

public enum FieldEntityType implements IntEnum {
  UNKNOWN(0),
  TEXT(1),
  MULTIPLE_CHOICE(2),
  PHOTO(3),
  NUMBER(4),
  DATE(5),
  TIME(6);

  private final int intValue;

  private static final ImmutableBiMap<FieldEntityType, Type> FIELD_TYPES =
      ImmutableBiMap.<FieldEntityType, Type>builder()
          .put(TEXT, Type.TEXT_FIELD)
          .put(MULTIPLE_CHOICE, Type.MULTIPLE_CHOICE)
          .put(PHOTO, Type.PHOTO)
          .put(NUMBER, Type.NUMBER)
          .put(DATE, Type.DATE)
          .put(TIME, Type.TIME).build();

  FieldEntityType(int intValue) {
    this.intValue = intValue;
  }

  @Override
  public int intValue() {
    return intValue;
  }

  public static FieldEntityType fromFieldType(Field.Type type) {
    return FIELD_TYPES.inverse().getOrDefault(type, FieldEntityType.UNKNOWN);
  }

  public Field.Type toFieldType() {
    return FIELD_TYPES.getOrDefault(this, Field.Type.UNKNOWN);
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
