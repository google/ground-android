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
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/** A user-provided response to a time question task. */
@Serializable
data class TimeTaskData(val formattedDate: @Contextual String, val time: Long) : TaskData {
  override fun getDetailsText(): String = formattedDate

  override fun isEmpty(): Boolean = time == 0L

  companion object {
    fun fromDate(formattedDate: String, time: Long): TaskData =
      TimeTaskData(formattedDate, time)
  }
}
