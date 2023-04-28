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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.MultipleChoiceCheckboxItemBinding
import com.google.android.ground.model.task.Option
import com.google.android.ground.ui.datacollection.tasks.multiplechoice.SelectMultipleOptionAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [Option]s with the [ViewHolder]
 * checkbox views.
 */
class SelectMultipleOptionAdapter(
  private val options: List<Option>,
  private val handleOptionSelected: (List<Option>) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

  private val selectedPositions = mutableSetOf<Int>()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    ViewHolder(
      MultipleChoiceCheckboxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(options[position])

    holder.binding.checkbox.isChecked = position in selectedPositions
    holder.binding.checkbox.setOnCheckedChangeListener { _, _ ->
      handleItemStateChanged(holder.adapterPosition)
    }
  }

  private fun handleItemStateChanged(position: Int) {
    if (position in selectedPositions) {
      selectedPositions.remove(position)
    } else {
      selectedPositions.add(position)
    }

    handleOptionSelected.invoke(
      options.filterIndexed { index, _ -> selectedPositions.contains(index) }
    )
  }

  override fun getItemCount(): Int = options.size

  class ViewHolder(internal val binding: MultipleChoiceCheckboxItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(option: Option) {
      binding.option = option
    }
  }
}
