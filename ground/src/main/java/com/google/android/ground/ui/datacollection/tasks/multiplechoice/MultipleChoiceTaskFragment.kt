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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.MultipleChoiceTaskFragBinding
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.material.divider.MaterialDividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment allowing the user to answer single selection multiple choice questions to complete a
 * task.
 */
@AndroidEntryPoint
class MultipleChoiceTaskFragment : AbstractTaskFragment<MultipleChoiceTaskViewModel>() {

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View =
    MultipleChoiceTaskFragBinding.inflate(inflater)
      .also { it.selectOptionList.setupRecyclerView() }
      .root

  private fun RecyclerView.setupRecyclerView() {
    adapter = createMultipleChoiceAdapter()
    itemAnimator = null
    setHasFixedSize(true)

    val itemDecoration = MaterialDividerItemDecoration(context, LinearLayoutManager.VERTICAL)
    itemDecoration.isLastItemDecorated = false
    addItemDecoration(itemDecoration)
  }

  private fun createMultipleChoiceAdapter(): MultipleChoiceAdapter {
    val cardinality = checkNotNull(getTask().multipleChoice).cardinality
    val canSelectMultiple = cardinality == MultipleChoice.Cardinality.SELECT_MULTIPLE
    val multipleChoiceAdapter = MultipleChoiceAdapter(viewModel, canSelectMultiple)
    lifecycleScope.launch {
      viewModel.itemsFlow.collect { items -> multipleChoiceAdapter.submitList(items) }
    }
    return multipleChoiceAdapter
  }
}
