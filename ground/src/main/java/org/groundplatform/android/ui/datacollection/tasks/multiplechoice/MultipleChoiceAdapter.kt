/*
 * Copyright 2024 Google LLC
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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.groundplatform.android.databinding.MultipleChoiceCheckboxItemBinding
import org.groundplatform.android.databinding.MultipleChoiceRadiobuttonItemBinding

/**
 * An implementation of [ListAdapter] that associates [MultipleChoiceItem]s with their [ViewHolder].
 */
class MultipleChoiceAdapter(
  private val viewModel: MultipleChoiceTaskViewModel,
  private val canSelectMultiple: Boolean,
) : ListAdapter<MultipleChoiceItem, RecyclerView.ViewHolder>(MultipleChoiceItemCallback) {

  override fun getItemViewType(position: Int): Int =
    if (canSelectMultiple) CHECKBOX_VIEW_TYPE else RADIO_VIEW_TYPE

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
    if (viewType == CHECKBOX_VIEW_TYPE)
      CheckBoxViewHolder(
        MultipleChoiceCheckboxItemBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false,
        )
      )
    else
      RadioButtonViewHolder(
        MultipleChoiceRadiobuttonItemBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false,
        )
      )

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
    (holder as MultipleChoiceViewHolder).bind(getItem(position), viewModel)

  private object MultipleChoiceItemCallback : DiffUtil.ItemCallback<MultipleChoiceItem>() {
    override fun areItemsTheSame(
      oldItem: MultipleChoiceItem,
      newItem: MultipleChoiceItem,
    ): Boolean = oldItem.isTheSameItem(newItem)

    override fun areContentsTheSame(
      oldItem: MultipleChoiceItem,
      newItem: MultipleChoiceItem,
    ): Boolean = oldItem.areContentsTheSame(newItem)
  }

  private interface MultipleChoiceViewHolder {
    fun bind(item: MultipleChoiceItem, viewModel: MultipleChoiceTaskViewModel)
  }

  class CheckBoxViewHolder(internal val binding: MultipleChoiceCheckboxItemBinding) :
    RecyclerView.ViewHolder(binding.root), MultipleChoiceViewHolder {
    override fun bind(item: MultipleChoiceItem, viewModel: MultipleChoiceTaskViewModel) {
      binding.item = item
      binding.viewModel = viewModel
    }
  }

  class RadioButtonViewHolder(internal val binding: MultipleChoiceRadiobuttonItemBinding) :
    RecyclerView.ViewHolder(binding.root), MultipleChoiceViewHolder {
    override fun bind(item: MultipleChoiceItem, viewModel: MultipleChoiceTaskViewModel) {
      binding.item = item
      binding.viewModel = viewModel
    }
  }

  companion object {
    private const val CHECKBOX_VIEW_TYPE = 1
    private const val RADIO_VIEW_TYPE = 2
  }
}
