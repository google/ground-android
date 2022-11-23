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
package com.google.android.ground.ui.datacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.BR
import com.google.android.ground.R
import com.google.android.ground.databinding.MultipleChoiceDataCollectionFragBinding
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.editsubmission.AbstractTaskViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment allowing the user to answer single selection multiple choice questions to complete a
 * task.
 */
@AndroidEntryPoint
class MultipleChoiceDataCollectionFragment
constructor(private val task: Task, private val viewModel: AbstractTaskViewModel) :
  AbstractFragment() {
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = MultipleChoiceDataCollectionFragBinding.inflate(inflater, container, false)

    binding.lifecycleOwner = this
    binding.setVariable(BR.viewModel, viewModel)

    val multipleChoice = task.multipleChoice!!
    if (multipleChoice.cardinality == MultipleChoice.Cardinality.SELECT_MULTIPLE) {
      // TODO(jsunde): Figure out why the Multiple Choice RecyclerView still scrolls back to the top
      //  then factor out any shared logic between these branches
      val optionAdapter = SelectMultipleOptionAdapter(multipleChoice.options)
      optionAdapter.setHasStableIds(true)
      val optionListView = binding.root.findViewById<RecyclerView>(R.id.select_one_option_list)
      optionListView.setHasFixedSize(true)
      optionListView.adapter = optionAdapter
    } else {
      val optionListView = binding.root.findViewById<RecyclerView>(R.id.select_one_option_list)
      optionListView.setHasFixedSize(true)
      optionListView.adapter = SelectOneOptionAdapter(multipleChoice.options)
    }

    return binding.root
  }
}
