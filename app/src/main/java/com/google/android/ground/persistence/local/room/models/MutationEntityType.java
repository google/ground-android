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
import com.google.android.ground.model.mutation.Mutation;
import com.google.android.ground.persistence.local.room.IntEnum;

/** Defines how Room represents mutation types in the remote sync queue in the local db. */
public enum MutationEntityType implements IntEnum {
  /** Indicates the field was missing or contained an unrecognized value. */
  UNKNOWN(0),

  /** Indicates a new entity should be created. */
  CREATE(1),

  /** Indicates an existing entity should be updated. */
  UPDATE(2),

  /** Indicates an existing entity should be marked for deletion. */
  DELETE(3);

  private final int intValue;

  MutationEntityType(int intValue) {
    this.intValue = intValue;
  }

  public static MutationEntityType fromMutationType(Mutation.Type type) {
    switch (type) {
      case CREATE:
        return CREATE;
      case UPDATE:
        return UPDATE;
      case DELETE:
        return DELETE;
      default:
        return UNKNOWN;
    }
  }

  public Mutation.Type toMutationType() {
    switch (this) {
      case CREATE:
        return Mutation.Type.CREATE;
      case UPDATE:
        return Mutation.Type.UPDATE;
      case DELETE:
        return Mutation.Type.DELETE;
      default:
        return Mutation.Type.UNKNOWN;
    }
  }

  public int intValue() {
    return intValue;
  }

  @TypeConverter
  public static int toInt(@Nullable MutationEntityType value) {
    return IntEnum.toInt(value, UNKNOWN);
  }

  @NonNull
  @TypeConverter
  public static MutationEntityType fromInt(int intValue) {
    return IntEnum.fromInt(values(), intValue, UNKNOWN);
  }
}
