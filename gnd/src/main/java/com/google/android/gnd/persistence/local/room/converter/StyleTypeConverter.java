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

package com.google.android.gnd.persistence.local.room.converter;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import com.google.android.gnd.model.job.Style;

public class StyleTypeConverter {

  @TypeConverter
  @Nullable
  public static String toString(@Nullable Style style) {
    return style == null ? null : style.getColor();
  }

  @TypeConverter
  @Nullable
  public static Style fromString(@Nullable String color) {
    return color == null ? null : Style.builder().setColor(color).build();
  }
}
