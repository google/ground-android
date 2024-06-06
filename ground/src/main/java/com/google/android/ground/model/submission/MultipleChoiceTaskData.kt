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

import com.google.android.ground.model.task.MultipleChoice
import kotlinx.serialization.Serializable

/** User responses to a select-one (radio) or select-multiple (checkbox) field. */
@Serializable
class MultipleChoiceTaskData(
  private val multipleChoice: MultipleChoice?,
  val selectedOptionIds: List<String>,
) : TaskData {

  // TODO: Make these inner classes non-static and access Task directly.
  override fun getDetailsText(): String =
    selectedOptionIds
      .mapNotNull { multipleChoice?.getOptionById(it) }
      .map { it.label }
      .sorted()
      .joinToString()

  override fun isEmpty(): Boolean = selectedOptionIds.isEmpty()

  override fun equals(other: Any?): Boolean {
    if (other is MultipleChoiceTaskData) {
      return selectedOptionIds == other.selectedOptionIds
    }
    return false
  }

  override fun hashCode(): Int = selectedOptionIds.hashCode()

  override fun toString(): String = selectedOptionIds.sorted().joinToString()

  companion object {
    fun fromList(multipleChoice: MultipleChoice?, ids: List<String>): TaskData? =
      if (ids.isEmpty()) {
        null
      } else {
        MultipleChoiceTaskData(multipleChoice, ids)
      }
  }
}
