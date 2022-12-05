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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.MultipleChoiceCheckboxItemBinding
import com.google.android.ground.model.task.Option
import com.google.android.ground.ui.datacollection.SelectMultipleOptionAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [Option]s with the [ViewHolder]
 * checkbox views.
 */
class SelectMultipleOptionAdapter(private val options: List<Option>) :
  SelectionAdapter<ViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    ViewHolder(
      MultipleChoiceCheckboxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

  private val selectedPositions = mutableSetOf<Int>()

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(options[position])

    holder.binding.checkbox.isChecked = position in selectedPositions
  }

  override fun getItemCount(): Int = options.size

  override fun getItemId(position: Int): Long = position.toLong()

  override fun getPosition(key: Long): Int = key.toInt()

  override fun handleItemStateChanged(position: Int, selected: Boolean) {
    if (position in selectedPositions) {
      selectedPositions.remove(position)
    } else {
      selectedPositions.add(position)
    }
  }

  class ViewHolder(internal val binding: MultipleChoiceCheckboxItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(option: Option) {
      binding.option = option
    }
  }
}
