/*
 * Copyright 2022 Google LLC
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

import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.MultipleChoiceTaskFragBinding
import com.google.android.ground.model.submission.MultipleChoiceResponse
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment allowing the user to answer single selection multiple choice questions to complete a
 * task.
 */
@AndroidEntryPoint
class MultipleChoiceTaskFragment : AbstractTaskFragment<MultipleChoiceTaskViewModel>() {
  private lateinit var binding: MultipleChoiceTaskFragBinding

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    binding = MultipleChoiceTaskFragBinding.inflate(inflater)
    setupMultipleChoice(binding.selectOptionList)
    return binding.root
  }

  private fun setupMultipleChoice(recyclerView: RecyclerView) {
    val multipleChoice = checkNotNull(getTask().multipleChoice)
    val isMultipleChoice = multipleChoice.cardinality == MultipleChoice.Cardinality.SELECT_MULTIPLE
    val options = multipleChoice.options
    val selectedIndices = getSelectedIndices(options)

    recyclerView.adapter =
      createAdapter(options, isMultipleChoice, selectedIndices) { viewModel.updateResponse(it) }
    recyclerView.setHasFixedSize(true)
  }

  private fun createAdapter(
    options: List<Option>,
    isMultipleChoice: Boolean,
    selectedIndices: List<Int>,
    updateResponse: (options: List<Option>) -> Unit,
  ): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
    return if (isMultipleChoice) {
      SelectMultipleOptionAdapter(options, selectedIndices) { updateResponse(it) }
    } else {
      assert(selectedIndices.size < 2) {
        "Expected size to be less than 2, found ${selectedIndices.size}"
      }
      val selectedIndex = if (selectedIndices.size == 1) selectedIndices[0] else -1
      SelectOneOptionAdapter(options, selectedIndex) { updateResponse(listOf(it)) }
    }
  }

  /** Returns a list of selected indices for the current task. */
  private fun getSelectedIndices(options: List<Option>): List<Int> {
    val selectedIds = (getCurrentValue() as? MultipleChoiceResponse)?.selectedOptionIds
    val optionIds = options.map { it.id }
    return selectedIds?.map { optionIds.indexOf(it) } ?: listOf()
  }
}
