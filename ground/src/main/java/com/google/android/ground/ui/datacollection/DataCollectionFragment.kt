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
package com.google.android.ground.ui.datacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.ground.AbstractActivity
import com.google.android.ground.R
import com.google.android.ground.databinding.DataCollectionFragBinding
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.Navigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment allowing the user to collect data to complete a task. */
@AndroidEntryPoint
class DataCollectionFragment : AbstractFragment(), BackPressListener {
  @Inject lateinit var navigator: Navigator
  @Inject lateinit var schedulers: Schedulers
  @Inject lateinit var viewPagerAdapterFactory: DataCollectionViewPagerAdapterFactory

  private lateinit var viewModel: DataCollectionViewModel
  private val args: DataCollectionFragmentArgs by navArgs()
  private lateinit var viewPager: ViewPager2

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(DataCollectionViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = DataCollectionFragBinding.inflate(inflater, container, false)
    viewPager = binding.root.findViewById(R.id.pager)
    viewPager.isUserInputEnabled = false
    viewPager.offscreenPageLimit = 1

    viewModel.loadSubmissionDetails(args)
    viewModel.submission.observe(viewLifecycleOwner) { submission: Loadable<Submission> ->
      submission.value().ifPresent {
        viewPager.adapter = viewPagerAdapterFactory.create(this, it.job.tasksSorted, viewModel)
      }
    }

    viewModel.currentPosition.observe(viewLifecycleOwner) { viewPager.currentItem = it }
    viewModel.currentTaskDataLiveData.observe(viewLifecycleOwner) {
      viewModel.currentTaskData = it.orElse(null)
    }

    binding.viewModel = viewModel
    binding.lifecycleOwner = this

    (activity as AbstractActivity?)?.setActionBar(binding.dataCollectionToolbar, showTitle = false)

    return binding.root
  }

  override fun onBack(): Boolean =
    if (viewPager.currentItem == 0) {
      // If the user is currently looking at the first step, allow the system to handle the
      // Back button. This calls finish() on this activity and pops the back stack.
      false
    } else {
      // Otherwise, select the previous step.
      viewModel.currentPosition.value = viewModel.currentPosition.value!! - 1
      true
    }
}
