/*
 * Copyright 2025 Google LLC
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

package org.groundplatform.android.persistence.local.room.converter

import androidx.room.TypeConverter
import org.groundplatform.android.proto.Survey

class GeneralAccessConverter {

  @TypeConverter fun toDbValue(access: Survey.GeneralAccess?): String? = access?.name

  @TypeConverter
  fun fromDbValue(dbValue: String?): Survey.GeneralAccess =
    dbValue?.let { safeName -> Survey.GeneralAccess.valueOf(safeName) }
      ?: Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED
}
