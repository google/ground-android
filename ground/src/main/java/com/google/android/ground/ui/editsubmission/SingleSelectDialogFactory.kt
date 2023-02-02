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
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import java8.util.Optional
import java8.util.function.Consumer

class SingleSelectDialogFactory(
  override val context: Context,
  override val title: String,
  override val multipleChoice: MultipleChoice,
  override val currentResponse: Optional<MultipleChoiceTaskData>,
  override val valueConsumer: Consumer<List<Option>>
) : SelectDialogFactory {
  private var checkedItem = -1

  override val selectedOptions: List<Option>
    get() =
      if (checkedItem >= 0) {
        listOf(getOption(checkedItem))
      } else {
        listOf()
      }

  override fun createDialogBuilder(): AlertDialog.Builder =
    super.createDialogBuilder().setSingleChoiceItems(labels, checkedItem) { dialog, which ->
      onSelect(dialog, which)
    }

  override fun initSelectedState() {
    checkedItem =
      currentResponse
        .flatMap { taskData: MultipleChoiceTaskData -> taskData.getFirstId() }
        .flatMap { id -> multipleChoice.getIndex(id) }
        .orElse(-1)
  }

  private fun onSelect(dialog: DialogInterface, which: Int) {
    if (checkedItem == which) {
      // Allow user to toggle values off by tapping selected item.
      checkedItem = -1
      (dialog as AlertDialog).listView.setItemChecked(which, false)
    } else {
      checkedItem = which
    }
  }
}
