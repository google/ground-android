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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.MultipleChoiceTaskFragBinding
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment allowing the user to answer single selection multiple choice questions to complete a
 * task.
 */
@AndroidEntryPoint
class MultipleChoiceTaskFragment : AbstractTaskFragment<MultipleChoiceTaskViewModel>() {
  private lateinit var binding: MultipleChoiceTaskFragBinding
  private lateinit var multipleChoiceAdapter:
    ListAdapter<MultipleChoiceItem, RecyclerView.ViewHolder>

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    // TODO: Replace with compose ux
    binding = MultipleChoiceTaskFragBinding.inflate(inflater)
    setupMultipleChoice(binding.selectOptionList)
    return binding.root
  }

  // TODO: Replace recycler view with compose ux
  private fun setupMultipleChoice(recyclerView: RecyclerView) {
    val multipleChoice = checkNotNull(getTask().multipleChoice)
    val canSelectMultiple = multipleChoice.cardinality == MultipleChoice.Cardinality.SELECT_MULTIPLE
    multipleChoiceAdapter = MultipleChoiceAdapter(viewModel, canSelectMultiple)
    recyclerView.apply {
      adapter = multipleChoiceAdapter
      itemAnimator = null
      setHasFixedSize(true)
    }
    lifecycleScope.launch {
      viewModel.itemsFlow.collect { items -> multipleChoiceAdapter.submitList(items) }
    }
  }
}
