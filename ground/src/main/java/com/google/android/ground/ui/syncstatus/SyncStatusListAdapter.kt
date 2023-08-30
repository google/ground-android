/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.ui.syncstatus

import android.content.Context
import android.text.format.DateFormat.getDateFormat
import android.text.format.DateFormat.getTimeFormat
import android.util.Pair
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.SyncStatusListItemBinding
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.ui.common.LocationOfInterestHelper
import java.text.DateFormat
import java8.util.Optional

internal class SyncStatusListAdapter(
  context: Context,
  private val locationOfInterestHelper: LocationOfInterestHelper
) : RecyclerView.Adapter<SyncStatusListAdapter.SyncStatusViewHolder>() {

  private var mutations: List<Pair<LocationOfInterest, Mutation>> = listOf()
  private val dateFormat: DateFormat = getDateFormat(context)
  private val timeFormat: DateFormat = getTimeFormat(context)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyncStatusViewHolder =
    SyncStatusViewHolder(
      SyncStatusListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

  override fun onBindViewHolder(viewHolder: SyncStatusViewHolder, position: Int) {
    // TODO: i18n; add user friendly names.
    // TODO: Use data binding.
    // TODO(#876): Improve L&F and layout.
    val pair = mutations[position]
    val locationOfInterest = pair.first
    val mutation = pair.second
    val text =
      StringBuilder()
        .append(mutation.type)
        .append(' ')
        .append(if (mutation is LocationOfInterestMutation) "LocationOfInterest" else "Submission")
        .append(' ')
        .append(dateFormat.format(mutation.clientTimestamp))
        .append(' ')
        .append(timeFormat.format(mutation.clientTimestamp))
        .append('\n')
        .append(locationOfInterestHelper.getLabel(Optional.of(locationOfInterest)))
        .append('\n')
        .append(locationOfInterestHelper.getSubtitle(Optional.of(locationOfInterest)))
        .append('\n')
        .append("Sync ")
        .append(mutation.syncStatus)
        .toString()
    viewHolder.binding.syncStatusText.text = text
  }

  override fun getItemCount(): Int = mutations.size

  fun update(mutations: List<Pair<LocationOfInterest, Mutation>>) {
    this.mutations = mutations
    notifyDataSetChanged()
  }

  class SyncStatusViewHolder(internal val binding: SyncStatusListItemBinding) :
    RecyclerView.ViewHolder(binding.root)
}
