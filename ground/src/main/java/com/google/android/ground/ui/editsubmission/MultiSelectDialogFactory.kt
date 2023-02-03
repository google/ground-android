/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.ui.editsubmission

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import java8.util.Optional
import java8.util.function.Consumer

class MultiSelectDialogFactory(
  override val context: Context,
  override val title: String,
  override val multipleChoice: MultipleChoice,
  override val currentResponse: Optional<MultipleChoiceTaskData>,
  override val valueConsumer: Consumer<List<Option>>
) : SelectDialogFactory {
  private var checkedItems: BooleanArray? = null

  override val selectedOptions: List<Option>
    get() = (0..size()).filter { value: Int -> checkedItems!![value] }.map { getOption(it) }

  override fun createDialogBuilder(): AlertDialog.Builder =
    super.createDialogBuilder().setMultiChoiceItems(labels, checkedItems) { _, _, _: Boolean -> }

  override fun initSelectedState() {
    checkedItems = BooleanArray(size())
    currentResponse.ifPresent { response: MultipleChoiceTaskData ->
      updateCurrentSelectedItems(response)
    }
  }

  private fun updateCurrentSelectedItems(response: MultipleChoiceTaskData) {
    (0..size()).forEach { checkedItems!![it] = response.isSelected(getOption(it)) }
  }
}
