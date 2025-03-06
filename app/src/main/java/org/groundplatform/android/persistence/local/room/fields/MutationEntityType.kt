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
package org.groundplatform.android.persistence.local.room.fields

import androidx.room.TypeConverter
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.persistence.local.room.IntEnum
import org.groundplatform.android.persistence.local.room.IntEnum.Companion.fromInt
import org.groundplatform.android.persistence.local.room.IntEnum.Companion.toInt

/** Defines how Room represents mutation types in the remote sync queue in the local db. */
enum class MutationEntityType(private val intValue: Int) : IntEnum {
  /** Indicates the field was missing or contained an unrecognized value. */
  UNKNOWN(0),

  /** Indicates a new entity should be created. */
  CREATE(1),

  /** Indicates an existing entity should be updated. */
  UPDATE(2),

  /** Indicates an existing entity should be marked for deletion. */
  DELETE(3);

  fun toMutationType() =
    when (this) {
      CREATE -> Mutation.Type.CREATE
      UPDATE -> Mutation.Type.UPDATE
      DELETE -> Mutation.Type.DELETE
      else -> Mutation.Type.UNKNOWN
    }

  override fun intValue() = intValue

  companion object {
    fun fromMutationType(type: Mutation.Type?) =
      when (type) {
        Mutation.Type.CREATE -> CREATE
        Mutation.Type.UPDATE -> UPDATE
        Mutation.Type.DELETE -> DELETE
        else -> UNKNOWN
      }

    @JvmStatic
    @TypeConverter
    fun toInt(value: MutationEntityType?) =
      toInt(
        value,
        org.groundplatform.android.persistence.local.room.fields.MutationEntityType.UNKNOWN,
      )

    @JvmStatic
    @TypeConverter
    fun fromInt(intValue: Int) =
      fromInt(
        entries.toTypedArray(),
        intValue,
        org.groundplatform.android.persistence.local.room.fields.MutationEntityType.UNKNOWN,
      )
  }
}
