/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.multiplechoice

import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task

data class MultipleChoiceItem(
  val option: Option,
  val type: Task.Type,
  val isSelected: Boolean = false,
  val isOtherOption: Boolean = false,
  val otherText: String = "",
) {
  fun isTheSameItem(otherItem: MultipleChoiceItem): Boolean = this.option.id == otherItem.option.id

  fun areContentsTheSame(otherItem: MultipleChoiceItem): Boolean =
    otherItem.isSelected == this.isSelected

  fun isMultipleChoice(): Boolean = this.type == Task.Type.MULTIPLE_CHOICE
}
