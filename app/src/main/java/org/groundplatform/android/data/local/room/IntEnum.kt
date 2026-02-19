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

package org.groundplatform.android.data.local.room

/**
 * Common interface for Java enums with explicitly defined int representations. This is used instead
 * of relying on enum ordinal values to prevent accidentally breaking backwards compatibility when
 * adding and/or removing new enum values in the future.
 */
interface IntEnum {
  fun intValue(): Int

  companion object {
    @JvmStatic
    fun <E : IntEnum> toInt(enumValue: E?, defaultValue: E): Int =
      enumValue?.intValue() ?: defaultValue.intValue()

    @JvmStatic
    fun <E> fromInt(values: Array<E>, intValue: Int, defaultValue: E): E
      where E : Enum<E>, E : IntEnum =
      values.firstOrNull { it.intValue() == intValue } ?: defaultValue
  }
}
