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
package com.google.android.ground.ui.datacollection.tasks.multiplechoice

import android.text.Editable
import android.text.TextWatcher
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.MultipleChoiceTaskData.Companion.fromList
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.task.MultipleChoice.Cardinality.SELECT_MULTIPLE
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull

class MultipleChoiceTaskViewModel @Inject constructor() : AbstractTaskViewModel() {

  private val _items: MutableStateFlow<List<MultipleChoiceItem>> = MutableStateFlow(emptyList())
  val itemsFlow: Flow<List<MultipleChoiceItem>> = _items.asStateFlow().filterNotNull()

  private val selectedIds: MutableSet<String> = mutableSetOf()
  private var otherText: String = ""

  val otherTextWatcher =
    object : TextWatcher {
      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        otherText = s.toString()
        // Set the other option.
        _items.value
          .firstOrNull { it.isOtherOption }
          ?.let {
            val selected =
              if (task.isRequired) {
                otherText != ""
              } else {
                true
              }
            setItem(it, selected, it.cardinality == SELECT_MULTIPLE)
          }
        updateResponse()
      }

      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // Not implemented.
      }

      override fun afterTextChanged(s: Editable) {
        // Not implemented.
      }
    }

  override fun initialize(job: Job, task: Task, taskData: TaskData?) {
    super.initialize(job, task, taskData)
    loadPendingSelections()
    updateMultipleChoiceItems()
  }

  fun setItem(item: MultipleChoiceItem, selection: Boolean, canSelectMultiple: Boolean) {
    if (!canSelectMultiple && selection) {
      selectedIds.clear()
    }
    if (selection) {
      selectedIds.add(item.option.id)
    } else {
      selectedIds.remove(item.option.id)
    }
    updateResponse()
    updateMultipleChoiceItems()
  }

  fun toggleItem(item: MultipleChoiceItem, canSelectMultiple: Boolean) {
    val wasSelected = selectedIds.contains(item.option.id)
    setItem(item, !wasSelected, canSelectMultiple)
  }

  fun updateResponse() {
    setValue(
      fromList(
        task.multipleChoice,
        selectedIds.map {
          // if the other option is selected grab the associated text save instead of otherId
          if (it == OTHER_ID) OTHER_PREFIX + otherText.trim() + OTHER_SUFFIX else it
        },
      )
    )
  }

  /* Reads the list of options in a multiple choice object and converts them to MultipleChoiceItems*/
  private fun updateMultipleChoiceItems() {
    val multipleChoice = checkNotNull(task.multipleChoice)
    val itemsFromOptions: MutableList<MultipleChoiceItem> = mutableListOf()

    itemsFromOptions.addAll(
      multipleChoice.options.map { option ->
        MultipleChoiceItem(option, multipleChoice.cardinality, selectedIds.contains(option.id))
      }
    )

    if (multipleChoice.hasOtherOption) {
      Option(OTHER_ID, "", "").let {
        itemsFromOptions.add(
          MultipleChoiceItem(
            it,
            multipleChoice.cardinality,
            selectedIds.contains(it.id),
            true,
            otherText,
          )
        )
      }
    }
    this._items.value = itemsFromOptions
  }

  /* Reads the saved task value and adds selected items to the selected list*/
  private fun loadPendingSelections() {
    val selectedOptionIds = (taskTaskData.value as? MultipleChoiceTaskData)?.selectedOptionIds
    val multipleChoice = checkNotNull(task.multipleChoice)
    val optionIds = multipleChoice.options.map { option -> option.id }
    selectedOptionIds?.forEach {
      // if the saved option id was the user inputted other text save the text to other text and add
      // other id to selected ids
      if (!optionIds.contains(it)) {
        otherText = it.removeSurrounding(OTHER_PREFIX, OTHER_SUFFIX)
        selectedIds.add(OTHER_ID)
      } else selectedIds.add(it)
    }
  }

  companion object {
    const val OTHER_ID: String = "OTHER_ID"
    const val OTHER_PREFIX: String = "[ "
    const val OTHER_SUFFIX: String = " ]"
  }
}
