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
package com.google.android.ground.ui.offlinebasemap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.OfflineBaseMapListItemBinding
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.ui.common.Navigator
import com.google.common.collect.ImmutableList

internal class OfflineAreaListAdapter(private val navigator: Navigator) :
  RecyclerView.Adapter<OfflineAreaListAdapter.ViewHolder>() {

  private var offlineAreas: ImmutableList<OfflineArea> = ImmutableList.of()

  class ViewHolder
  internal constructor(
    var binding: OfflineBaseMapListItemBinding,
    var areas: ImmutableList<OfflineArea>,
    private val navigator: Navigator
  ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
    init {
      binding.offlineAreaName.setOnClickListener(this)
    }

    override fun onClick(v: View) {
      if (areas.size > 0) {
        val id = areas[adapterPosition].id
        navigator.navigate(OfflineAreasFragmentDirections.viewOfflineArea(id))
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val offlineAreasListItemBinding =
      OfflineBaseMapListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(offlineAreasListItemBinding, offlineAreas, navigator)
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    viewHolder.areas = offlineAreas
    viewHolder.binding.offlineAreaName.text = offlineAreas[position].name
  }

  override fun getItemCount(): Int {
    return offlineAreas.size
  }

  fun update(offlineAreas: ImmutableList<OfflineArea>) {
    this.offlineAreas = offlineAreas
    notifyDataSetChanged()
  }
}
