/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.ui.home.locationofinterestdetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.SubmissionListItemBinding
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.ViewModelFactory

// TODO: Consider passing in ViewModel and using DataBinding like todoapp example.
internal class SubmissionListAdapter(private val viewModelFactory: ViewModelFactory) :
  RecyclerView.Adapter<SubmissionListItemViewHolder>() {

  private var submissionList: List<Submission>
  private val itemClicks: @Hot(replays = true) MutableLiveData<Submission> = MutableLiveData()

  init {
    submissionList = emptyList()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionListItemViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val itemBinding = SubmissionListItemBinding.inflate(inflater, parent, false)
    // Note: Before using Android Data Binding for this component, holder must implement
    // LifecycleOwner and be passed to itemBinding.setLifecycleOwner().
    return SubmissionListItemViewHolder(itemBinding)
  }

  override fun onBindViewHolder(holder: SubmissionListItemViewHolder, position: Int) {
    val viewModel = viewModelFactory.create(SubmissionListItemViewModel::class.java)
    viewModel.setSubmission(submissionList[position])
    viewModel.setSubmissionCallback { itemClicks.postValue(it) }
    holder.bind(viewModel, submissionList[position])
  }

  override fun getItemCount(): Int = submissionList.size

  fun getItemClicks(): LiveData<Submission> = itemClicks

  fun clear() {
    submissionList = emptyList()
    notifyDataSetChanged()
  }

  fun update(submissionList: List<Submission>) {
    this.submissionList = submissionList
    notifyDataSetChanged()
  }
}
