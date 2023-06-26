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

import static java8.util.J8Arrays.stream;

import androidx.annotation.NonNull;

/**
 * Common interface for Java enums with explicitly defined int representations. This is used instead
 * of relying on enum ordinal values to prevent accidentally breaking backwards compatibility when
 * adding and/or removing new enum values in the future.
 */
public interface IntEnum {
  int intValue();

  static <E extends IntEnum> int toInt(E enumValue, @NonNull E defaultValue) {
    return enumValue == null ? defaultValue.intValue() : enumValue.intValue();
  }

  @NonNull
  static <E extends Enum<E> & IntEnum> E fromInt(
      @NonNull E[] values, int intValue, @NonNull E defaultValue) {
    return stream(values).filter(s -> s.intValue() == intValue).findFirst().orElse(defaultValue);
  }
}
