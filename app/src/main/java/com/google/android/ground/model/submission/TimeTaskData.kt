/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.model.submission

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java8.util.Optional
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/** A user-provided time taskData. */
@Serializable
data class TimeTaskData(val time: @Contextual Date) : TaskData {
  // TODO(#752): Use device localization preferences.
  private val timeFormat: @Contextual DateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

  override fun getDetailsText(): String =
    synchronized(timeFormat) {
      return timeFormat.format(time)
    }

  override fun isEmpty(): Boolean = time.time == 0L

  companion object {
    @JvmStatic
    fun fromDate(time: Date): Optional<TaskData> =
      if (time.time == 0L) Optional.empty() else Optional.of(TimeTaskData(time))
  }
}
