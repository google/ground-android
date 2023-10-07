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
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment allowing the user to answer single selection multiple choice questions to complete a
 * task.
 */
@AndroidEntryPoint(AbstractTaskFragment::class)
class MultipleChoiceTaskFragment : Hilt_MultipleChoiceTaskFragment<MultipleChoiceTaskViewModel>() {
  private lateinit var binding: MultipleChoiceTaskFragBinding

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    binding = MultipleChoiceTaskFragBinding.inflate(inflater)
    setupMultipleChoice(binding.selectOptionList)
    return binding.root
  }

  override fun skip() {
    super.skip()

    setupMultipleChoice(binding.selectOptionList)
  }

  private fun setupMultipleChoice(recyclerView: RecyclerView) {
    val multipleChoice = viewModel.task.multipleChoice!!
    recyclerView.setHasFixedSize(true)
    if (multipleChoice.cardinality == MultipleChoice.Cardinality.SELECT_MULTIPLE) {
      recyclerView.adapter =
        SelectMultipleOptionAdapter(multipleChoice.options) { viewModel.updateResponse(it) }
    } else {
      recyclerView.adapter =
        SelectOneOptionAdapter(multipleChoice.options) { viewModel.updateResponse(listOf(it)) }
    }
  }
}
