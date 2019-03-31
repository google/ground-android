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
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.room.TypeConverter;
import com.google.android.gnd.vo.Record.Value;

/** Fields describing operation type, old, and new values, common to Feature and Record edits. */
public class Edit {
  public enum Type implements IntEnum {
    UNKNOWN(0),
    CREATE(1),
    UPDATE(2),
    DELETE(3);

    private final int intValue;

    Type(int intValue) {
      this.intValue = intValue;
    }

    public int intValue() {
      return intValue;
    }

    @TypeConverter
    public static int toInt(@Nullable Edit.Type value) {
      return IntEnum.fromIntEnum(value, UNKNOWN);
    }

    @NonNull
    @TypeConverter
    public static Edit.Type fromInt(int intValue) {
      return IntEnum.toIntEnum(stream(values()), intValue, UNKNOWN);
    }
  }

  @ColumnInfo(name = "type")
  @NonNull
  public Type type;

  /** When null the edit refers to the entity itself. */
  @ColumnInfo(name = "key")
  @Nullable
  public String key;

  // TODO: Add Converter to convert to/from JSON.
  @Ignore @Nullable public Value oldValue;

  // TODO: Add Converter to convert to/from JSON.
  @Ignore @Nullable public Value newValue;
}
