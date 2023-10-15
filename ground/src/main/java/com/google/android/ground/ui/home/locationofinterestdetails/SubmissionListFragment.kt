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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.ground.databinding.SubmissionListFragBinding
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import dagger.hilt.android.AndroidEntryPoint
import java8.util.Optional
import javax.inject.Inject

@AndroidEntryPoint(AbstractFragment::class)
class SubmissionListFragment : Hilt_SubmissionListFragment() {

  @Inject lateinit var navigator: Navigator

  private lateinit var binding: SubmissionListFragBinding
  private lateinit var locationOfInterestDetailsViewModel: LocationOfInterestDetailsViewModel
  private lateinit var submissionListAdapter: SubmissionListAdapter
  private lateinit var viewModel: SubmissionListViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    submissionListAdapter = SubmissionListAdapter(viewModelFactory)
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SubmissionListViewModel::class.java)
    locationOfInterestDetailsViewModel =
      getViewModel(LocationOfInterestDetailsViewModel::class.java)
    submissionListAdapter.getItemClicks().observe(this) { submission: Submission ->
      onItemClick(submission)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = SubmissionListFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val submissionList = binding.submissionListContainer
    submissionList.layoutManager = LinearLayoutManager(context)
    submissionList.adapter = submissionListAdapter
    locationOfInterestDetailsViewModel.getSelectedLocationOfInterestOnceAndStream().observe(
      viewLifecycleOwner
    ) {
      onLocationOfInterestSelected(it)
    }
  }

  private fun onLocationOfInterestSelected(locationOfInterest: Optional<LocationOfInterest>) {
    submissionListAdapter.clear()
    locationOfInterest.ifPresent { viewModel.loadSubmissionList(it) }
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    viewModel.submissions.observe(viewLifecycleOwner) { submissionListAdapter.update(it) }
  }

  private fun onItemClick(submission: Submission) {
    navigator.navigate(
      HomeScreenFragmentDirections.showSubmissionDetails(
        submission.surveyId,
        submission.locationOfInterest.id,
        submission.id
      )
    )
  }
}
