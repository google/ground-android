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
import androidx.recyclerview.selection.*
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.BR
import com.google.android.ground.R
import com.google.android.ground.databinding.MultipleChoiceTaskFragBinding
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.editsubmission.AbstractTaskViewModel
import com.google.android.ground.ui.editsubmission.MultipleChoiceTaskViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment allowing the user to answer single selection multiple choice questions to complete a
 * task.
 */
@AndroidEntryPoint
class MultipleChoiceTaskFragment : AbstractFragment(), TaskFragment {
  override lateinit var viewModel: AbstractTaskViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = MultipleChoiceTaskFragBinding.inflate(inflater, container, false)

    binding.lifecycleOwner = this
    binding.setVariable(BR.viewModel, viewModel)

    val multipleChoiceTaskViewModel = viewModel as MultipleChoiceTaskViewModel

    val multipleChoice = viewModel.task.multipleChoice!!
    val optionListView = binding.root.findViewById<RecyclerView>(R.id.select_option_list)
    optionListView.setHasFixedSize(true)
    if (multipleChoice.cardinality == MultipleChoice.Cardinality.SELECT_MULTIPLE) {
      val adapter = SelectMultipleOptionAdapter(multipleChoice.options, multipleChoiceTaskViewModel)
      adapter.setHasStableIds(true)
      optionListView.adapter = adapter
      setupMultipleSelectionTracker(optionListView, adapter)
    } else {
      optionListView.adapter =
        SelectOneOptionAdapter(multipleChoice.options, multipleChoiceTaskViewModel)
    }

    return binding.root
  }

  private fun setupMultipleSelectionTracker(view: RecyclerView, adapter: SelectionAdapter<*>) {
    val itemKeyProvider =
      object : ItemKeyProvider<Long>(SCOPE_CACHED) {
        override fun getKey(position: Int): Long = adapter.getItemId(position)

        override fun getPosition(key: Long): Int = key.toInt()
      }

    val itemDetailsLookup: ItemDetailsLookup<Long> = OptionListItemDetailsLookup(view)

    val selectionTracker =
      SelectionTracker.Builder(
          "option_selection",
          view,
          itemKeyProvider,
          itemDetailsLookup,
          StorageStrategy.createLongStorage()
        )
        .build()

    selectionTracker.addObserver(
      object : SelectionObserver<Long>() {
        override fun onItemStateChanged(key: Long, selected: Boolean) {
          adapter.handleItemStateChanged(key.toInt(), selected)
        }
      }
    )
  }
}
