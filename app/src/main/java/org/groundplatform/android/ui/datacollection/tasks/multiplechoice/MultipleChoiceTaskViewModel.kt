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
package org.groundplatform.android.ui.datacollection.tasks.multiplechoice

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.MultipleChoiceTaskData
import org.groundplatform.android.model.submission.MultipleChoiceTaskData.Companion.fromList
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.MultipleChoice.Cardinality.SELECT_MULTIPLE
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel

class MultipleChoiceTaskViewModel @Inject constructor() : AbstractTaskViewModel() {

  private val _items: MutableStateFlow<List<MultipleChoiceItem>> = MutableStateFlow(emptyList())
  val itemsFlow: Flow<List<MultipleChoiceItem>> = _items.asStateFlow().filterNotNull()

  private val selectedIds: MutableSet<String> = mutableSetOf()
  private var otherText: String = ""

  override fun initialize(job: Job, task: Task, taskData: TaskData?) {
    super.initialize(job, task, taskData)
    loadPendingSelections()
    updateMultipleChoiceItems()
  }

  private fun setItem(item: MultipleChoiceItem, selection: Boolean) {
    if (item.cardinality != SELECT_MULTIPLE && selection) {
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

  /**
   * Invoked when "other" text input field is modified. It updates the internal state with the new
   * text, if valid.
   *
   * @param text new text entered in the "other" input field.
   */
  fun onOtherTextChanged(text: String) {
    otherText = text
    // Set the other option.
    _items.value
      .firstOrNull { it.isOtherOption }
      ?.let { setItem(item = it, selection = isOtherTextValid()) }
    updateResponse()
  }

  /**
   * Invoked when a multiple choice item is selected/unselected.
   *
   * @param item multiple choice item which was modified.
   */
  fun onItemToggled(item: MultipleChoiceItem) {
    val wasSelected = selectedIds.contains(item.option.id)
    setItem(item, !wasSelected)
  }

  private fun updateResponse() {
    // Check if "other" text is missing or not.
    if (selectedIds.contains(OTHER_ID) && !isOtherTextValid()) {
      clearResponse()
      return
    }

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

  /** Returns true if the value for "other" text is valid, otherwise false. */
  private fun isOtherTextValid(): Boolean {
    if (!task.isRequired) {
      // If task is not required, then the text value can be empty. Hence, it is always valid.
      return true
    }

    return otherText.isNotBlank()
  }

  /**
   * Reads the list of options in a multiple choice object and converts them to MultipleChoiceItems.
   */
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
    this._items.update { itemsFromOptions }
  }

  /** Reads the saved task value and adds selected items to the selected list. */
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
