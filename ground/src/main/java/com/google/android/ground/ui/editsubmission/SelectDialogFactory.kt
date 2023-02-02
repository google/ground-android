/*
 * Copyright 2020 Google LLC
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
import com.google.android.ground.R
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import java8.util.Optional
import java8.util.function.Consumer

interface SelectDialogFactory{
  val selectedOptions: List<Option>
  val context: Context
  val title: String
  val multipleChoice: MultipleChoice
  val currentResponse: Optional<MultipleChoiceTaskData>
  val valueConsumer: Consumer<List<Option>>

  private val options: List<Option>
    get() = multipleChoice.options

  val labels: Array<String>
    get() = multipleChoice.options.map { it.label }.toTypedArray()

  fun getOption(index: Int): Option = options[index]

  fun size(): Int = options.size

  // TODO: Replace with modal bottom sheet.
  fun createDialogBuilder(): AlertDialog.Builder =
    AlertDialog.Builder(context)
      .setCancelable(false)
      .setTitle(title)
      .setPositiveButton(R.string.apply_multiple_choice_changes) { _, _ ->
        valueConsumer.accept(selectedOptions)
      }
      .setNegativeButton(R.string.cancel) { _, _ -> }

  fun createDialog(): AlertDialog {
    initSelectedState()
    return createDialogBuilder().create()
  }

  /** Creates and displays the dialog. */
  fun show() = createDialog().show()

  /** Initialize current state. */
  fun initSelectedState()
}
