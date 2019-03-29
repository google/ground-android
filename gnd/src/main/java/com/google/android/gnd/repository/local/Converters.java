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

package com.google.android.gnd.repository.local;

import static java8.util.J8Arrays.stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import java8.util.stream.Stream;

/**
 * Room type converters required to (de)serialize custom objects to/from database.
 */
final class Converters {

  private Converters() {
    // DO NOT INSTANTIATE.
  }

  @TypeConverter
  public static int fromEditType(@Nullable Edit.Type value) {
    return fromIntEnum(value, Edit.Type.UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static Edit.Type toEditType(int intValue) {
    return toIntEnum(stream(Edit.Type.values()), intValue, Edit.Type.UNKNOWN);
  }

  @TypeConverter
  public static int fromEntityState(@Nullable EntityState value) {
    return fromIntEnum(value, EntityState.UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static EntityState toEntityState(int intValue) {
    return toIntEnum(stream(EntityState.values()), intValue, EntityState.UNKNOWN);
  }

  private static <E extends IntEnum> int fromIntEnum(E enumValue, @NonNull E defaultValue) {
    return enumValue == null ? defaultValue.intValue() : enumValue.intValue();
  }

  @NonNull
  private static <E extends Enum<E> & IntEnum> E toIntEnum(
      @NonNull Stream<E> values, int intValue, @NonNull E defaultValue) {
    return values.filter(s -> s.intValue() == intValue).findFirst().orElse(defaultValue);
  }
}
