/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.ui.offlineareas

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.OfflineAreasListItemBinding

internal class OfflineAreaListAdapter : RecyclerView.Adapter<OfflineAreaListAdapter.ViewHolder>() {

  private var offlineAreas: List<OfflineAreaListItemViewModel> = listOf()

  class ViewHolder internal constructor(var binding: OfflineAreasListItemBinding) :
    RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val offlineAreasListItemBinding =
      OfflineAreasListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(offlineAreasListItemBinding)
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    viewHolder.binding.viewModel = offlineAreas[position]
  }

  override fun getItemCount(): Int = offlineAreas.size

  @SuppressLint("NotifyDataSetChanged")
  fun update(offlineAreas: List<OfflineAreaListItemViewModel>) {
    this.offlineAreas = offlineAreas
    notifyDataSetChanged()
  }
}
