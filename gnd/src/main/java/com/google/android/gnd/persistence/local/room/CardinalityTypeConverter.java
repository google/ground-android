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

package com.google.android.gnd.persistence.local.room;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;
import com.google.android.gnd.model.form.MultipleChoice.Cardinality;

public class CardinalityTypeConverter {

  @TypeConverter
  @NonNull
  public static String toString(Cardinality cardinality) {
    return cardinality.name();
  }

  @TypeConverter
  @NonNull
  public static Cardinality fromString(String value) {
    return Cardinality.valueOf(value);
  }
}
