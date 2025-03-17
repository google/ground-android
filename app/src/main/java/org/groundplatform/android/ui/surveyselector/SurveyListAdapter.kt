/*
 * Copyright 2023 Google LLC
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

package org.groundplatform.android.ui.surveyselector

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.groundplatform.android.databinding.SurveyCardItemBinding
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.ui.surveyselector.SurveyListAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [SurveyListItem] data with the
 * [ViewHolder] views.
 */
class SurveyListAdapter(
  private val viewModel: SurveySelectorViewModel,
  private val fragment: SurveySelectorFragment,
) : RecyclerView.Adapter<ViewHolder>() {

  private val surveys: MutableList<SurveyListItem> = mutableListOf()

  /** Creates a new [ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = SurveyCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding)
  }

  /** Binds [SurveyListItem] data to [ViewHolder]. */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val surveyListItem = surveys[position]
    with(holder.binding) {
      val me = this@SurveyListAdapter
      item = surveyListItem
      viewModel = me.viewModel
      fragment = me.fragment

      val offlineVisibility = if (surveyListItem.availableOffline) View.VISIBLE else View.GONE
      offlineIcon.visibility = offlineVisibility
      overflowMenu.visibility = offlineVisibility
    }
  }

  /** Returns the size of the list. */
  override fun getItemCount() = surveys.size

  /** Overwrites existing cards. */
  fun updateData(newItemsList: List<SurveyListItem>) {
    surveys.clear()
    surveys.addAll(newItemsList)
    notifyDataSetChanged()
  }

  /** View item representing the [SurveyListItem] data in the list. */
  class ViewHolder(internal val binding: SurveyCardItemBinding) :
    RecyclerView.ViewHolder(binding.root)
}
