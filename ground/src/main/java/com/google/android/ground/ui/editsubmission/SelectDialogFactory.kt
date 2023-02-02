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

abstract class SelectDialogFactory {
  private val options: List<Option>
    get() = multipleChoice.options

  protected fun getOption(index: Int): Option = options[index]

  protected fun size(): Int = options.size

  protected val labels: Array<String>
    get() = multipleChoice.options.map { it.label }.toTypedArray()

  // TODO: Replace with modal bottom sheet.
  protected open fun createDialogBuilder(): AlertDialog.Builder =
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
  protected fun show() {
    createDialog().show()
  }

  /** Initialize current state. */
  protected abstract fun initSelectedState()

  /** List of selected options. */
  protected abstract val selectedOptions: List<Option>
  abstract val context: Context
  abstract val title: String
  abstract val multipleChoice: MultipleChoice
  abstract val currentResponse: Optional<MultipleChoiceTaskData>
  abstract val valueConsumer: Consumer<List<Option>>

  abstract class Builder<B> {
    abstract fun setContext(context: Context): B
    abstract fun setTitle(title: String): B
    abstract fun setMultipleChoice(multipleChoice: MultipleChoice): B
    abstract fun setCurrentResponse(response: Optional<MultipleChoiceTaskData>): B
    abstract fun setValueConsumer(consumer: Consumer<List<Option>>): B
  }
}
