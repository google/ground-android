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

package com.google.android.ground.ui.home.mapcontainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.ui.home.mapcontainer.LoiCardAdapter.ViewHolder
import com.google.android.ground.ui.map.LoiCard

/**
 * An implementation of [RecyclerView.Adapter] that associates [LoiCard] data with the [ViewHolder]
 * views.
 */
class LoiCardAdapter : RecyclerView.Adapter<ViewHolder>() {

  private val itemsList: MutableList<LoiCard> = mutableListOf()

  /** Creates a new [ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.loi_card_item, parent, false)
    return ViewHolder(view)
  }

  /** Binds [LoiCard] data to [ViewHolder]. */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val itemsViewModel = itemsList[position]
    holder.loiId.text = itemsViewModel.loiId
    holder.loiName.text = itemsViewModel.loiName
    holder.jobName.text = itemsViewModel.jobName
  }

  /** Returns the size of the list. */
  override fun getItemCount() = itemsList.size

  fun updateData(newItemsList: List<LoiCard>) {
    itemsList.clear()
    itemsList.addAll(newItemsList)
    notifyDataSetChanged()
  }

  /** View item representing the [LoiCard] data in the list. */
  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val loiId: TextView = view.findViewById(R.id.loiId)
    val loiName: TextView = view.findViewById(R.id.loiName)
    val jobName: TextView = view.findViewById(R.id.jobName)
  }
}
