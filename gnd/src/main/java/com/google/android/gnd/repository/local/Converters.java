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

public abstract class Converters {

  public Converters() {
    // DO NOT INSTANTIATE.
  }

  @TypeConverter
  private static int fromSnapshotType(@Nullable SnapshotType value) {
    return fromIntEnum(value, SnapshotType.UNKNOWN);
  }

  @TypeConverter
  private static int fromEditType(@Nullable EditType value) {
    return fromIntEnum(value, EditType.UNKNOWN);
  }

  @TypeConverter
  private static int fromDeletionState(@Nullable DeletionState value) {
    return fromIntEnum(value, DeletionState.UNKNOWN);
  }

  private static <E extends IntEnum> int fromIntEnum(E enumValue, @NonNull E defaultValue) {
    return enumValue == null ? defaultValue.intValue() : enumValue.intValue();
  }

  @NonNull
  @TypeConverter
  public static SnapshotType toSnapshotType(int intValue) {
    return toIntEnum(stream(SnapshotType.values()), intValue, SnapshotType.UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static EditType toEditType(int intValue) {
    return toIntEnum(stream(EditType.values()), intValue, EditType.UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static DeletionState toDeletionState(int intValue) {
    return toIntEnum(stream(DeletionState.values()), intValue, DeletionState.UNKNOWN);
  }

  @NonNull
  private static <E extends Enum<E> & IntEnum> E toIntEnum(
      @NonNull Stream<E> values, int intValue, @NonNull E defaultValue) {
    return values.filter(s -> s.intValue() == intValue).findFirst().orElse(defaultValue);
  }
}
