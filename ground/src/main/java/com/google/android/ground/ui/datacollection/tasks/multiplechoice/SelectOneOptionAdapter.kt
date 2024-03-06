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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.MultipleChoiceRadiobuttonItemBinding
import com.google.android.ground.model.task.Option
import com.google.android.ground.ui.datacollection.tasks.multiplechoice.SelectOneOptionAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [Option] data with the [ViewHolder]
 * RadioButton views.
 */
class SelectOneOptionAdapter(
  private val options: List<Option>,
  private val currentlySelectedPosition: Int,
  private val handleOptionSelected: (Option) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

  private var selectedIndex = currentlySelectedPosition

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    ViewHolder(
      MultipleChoiceRadiobuttonItemBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
    )

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(options[position])

    holder.binding.radioButton.isChecked = position == selectedIndex
    holder.binding.radioButton.setOnClickListener {
      handleItemStateChange(it, holder.adapterPosition)
    }
  }

  private fun handleItemStateChange(view: View, position: Int) {
    val oldPosition = selectedIndex
    selectedIndex = position
    handleOptionSelected(options[selectedIndex])

    view.post {
      if (oldPosition >= 0) {
        notifyItemChanged(oldPosition)
      }
      notifyItemChanged(selectedIndex)
    }
  }

  override fun getItemCount(): Int = options.size

  class ViewHolder(internal val binding: MultipleChoiceRadiobuttonItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(option: Option) {
      binding.option = option
    }
  }
}
