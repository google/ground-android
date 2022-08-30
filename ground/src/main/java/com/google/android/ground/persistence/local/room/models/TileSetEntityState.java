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
import com.google.android.ground.persistence.local.room.IntEnum;

public enum TileSetEntityState implements IntEnum {
  UNKNOWN(0),
  PENDING(1),
  IN_PROGRESS(2),
  DOWNLOADED(3),
  FAILED(4);

  private final int intValue;

  TileSetEntityState(int intValue) {
    this.intValue = intValue;
  }

  public int intValue() {
    return intValue;
  }

  @TypeConverter
  public static int toInt(@Nullable TileSetEntityState value) {
    return IntEnum.toInt(value, UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static TileSetEntityState fromInt(int intValue) {
    return IntEnum.fromInt(values(), intValue, UNKNOWN);
  }
}
