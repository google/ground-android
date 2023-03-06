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
package com.google.android.ground.model.submission

import java8.util.Optional
import kotlinx.serialization.Serializable

/** A user provided taskData to a text question task. */
@Serializable
data class TextTaskData(val text: String) : TaskData {
  override fun getDetailsText(): String = text

  override fun isEmpty(): Boolean = text.trim { it <= ' ' }.isEmpty()

  companion object {
    @JvmStatic
    fun fromString(text: String): Optional<TaskData> =
      if (text.isEmpty()) Optional.empty() else Optional.of(TextTaskData(text))
  }
}
