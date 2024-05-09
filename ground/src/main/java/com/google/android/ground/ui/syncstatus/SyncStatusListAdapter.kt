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
import android.graphics.drawable.Drawable
import android.text.format.DateFormat.getDateFormat
import android.text.format.DateFormat.getTimeFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.SyncStatusListItemBinding
import com.google.android.ground.model.mutation.Mutation
import java.text.DateFormat

internal class SyncStatusListAdapter(private val context: Context) :
  RecyclerView.Adapter<SyncStatusListAdapter.SyncStatusViewHolder>() {

  private var mutations: List<MutationDetail> = listOf()
  private val dateFormat: DateFormat = getDateFormat(context)
  private val timeFormat: DateFormat = getTimeFormat(context)
  private val pendingIcon: Drawable? = getDrawable(R.drawable.baseline_hourglass_empty_24)
  private val syncingIcon: Drawable? = getDrawable(R.drawable.ic_sync)
  private val mediaPending: Drawable? = getDrawable(R.drawable.baseline_check_24)
  private val completeIcon: Drawable? = getDrawable(R.drawable.outline_done_all_24)
  private val failedIcon: Drawable? = getDrawable(R.drawable.outline_error_outline_24)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyncStatusViewHolder =
    SyncStatusViewHolder(
      SyncStatusListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

  override fun onBindViewHolder(viewHolder: SyncStatusViewHolder, position: Int) {
    // TODO: i18n; add user friendly names.
    // TODO: Use data binding.
    // TODO(#876): Improve L&F and layout.
    val detail = mutations[position]
    val modified =
      "${dateFormat.format(detail.mutation.clientTimestamp)} . ${timeFormat.format(detail.mutation.clientTimestamp)}"
    viewHolder.binding.mutationTimestamp.text = modified
    viewHolder.binding.mutationStatus.text = detail.mutation.syncStatus.toString()
    viewHolder.binding.mutationUser.text = detail.user
    viewHolder.binding.mutationSurvey.text = detail.loiLabel
    viewHolder.binding.mutationLoi.text = detail.loiSubtitle
    when (detail.mutation.syncStatus) {
      Mutation.SyncStatus.PENDING -> viewHolder.setDrawable(pendingIcon)
      Mutation.SyncStatus.IN_PROGRESS -> viewHolder.setDrawable(syncingIcon)
      Mutation.SyncStatus.MEDIA_UPLOAD_PENDING -> viewHolder.setDrawable(mediaPending)
      Mutation.SyncStatus.COMPLETED -> viewHolder.setDrawable(completeIcon)
      Mutation.SyncStatus.FAILED -> viewHolder.setDrawable(failedIcon)
      else -> {}
    }
  }

  override fun getItemCount(): Int = mutations.size

  fun update(mutations: List<MutationDetail>) {
    this.mutations = mutations
    notifyDataSetChanged()
  }

  class SyncStatusViewHolder(internal val binding: SyncStatusListItemBinding) :
    RecyclerView.ViewHolder(binding.root)

  private fun getDrawable(id: Int) =
    ResourcesCompat.getDrawable(context.resources, id, context.theme)

  private fun SyncStatusViewHolder.setDrawable(drawable: Drawable?) =
    this.binding.mutationStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(
      null,
      null,
      drawable,
      null,
    )
}
