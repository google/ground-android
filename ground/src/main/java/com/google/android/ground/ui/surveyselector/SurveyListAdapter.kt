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

package com.google.android.ground.ui.surveyselector

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.SurveyCardItemBinding
import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.ui.surveyselector.SurveyListAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [Survey] data with the [ViewHolder]
 * views.
 */
class SurveyListAdapter(private val viewModel: SurveySelectorViewModel) :
  RecyclerView.Adapter<ViewHolder>() {

  private val surveys: MutableList<Survey> = mutableListOf()

  /** Creates a new [ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = SurveyCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding)
  }

  /** Binds [LocationOfInterest] data to [ViewHolder]. */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val survey: Survey = surveys[position]
    holder.bind(survey, viewModel)
  }

  /** Returns the size of the list. */
  override fun getItemCount() = surveys.size

  /** Overwrites existing cards. */
  fun updateData(newItemsList: List<Survey>) {
    surveys.clear()
    surveys.addAll(newItemsList)
    notifyDataSetChanged()
  }

  /** View item representing the [Survey] data in the list. */
  class ViewHolder(internal val binding: SurveyCardItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(survey: Survey, viewModel: SurveySelectorViewModel) {
      binding.survey = survey
      binding.viewModel = viewModel
      binding.description.visibility = if (survey.description.isEmpty()) View.GONE else View.VISIBLE
    }
  }
}
