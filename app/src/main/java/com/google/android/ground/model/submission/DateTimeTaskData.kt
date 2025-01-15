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

import kotlinx.serialization.Serializable

/** A user-provided response to a date and time question task. */
@Serializable
data class DateTimeTaskData(val timeInMillis: Long) : TaskData {

  override fun isEmpty(): Boolean = timeInMillis == 0L

  companion object {
    fun fromMillis(timeInMillis: Long): TaskData = DateTimeTaskData(timeInMillis)
  }
}
