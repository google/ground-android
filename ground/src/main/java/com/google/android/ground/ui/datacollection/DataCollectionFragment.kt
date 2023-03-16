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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.ground.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.google.android.ground.databinding.DataCollectionFragBinding
import androidx.compose.ui.unit.dp
import com.google.android.ground.model.submission.Submission
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

  private val args: DataCollectionFragmentArgs by navArgs()
  private val viewModel: DataCollectionViewModel by activityViewModels()

  private lateinit var viewPager: ViewPager2

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
    viewModel.submission.observe(viewLifecycleOwner) { submission: Submission ->
      val tasks = submission.job.tasksSorted
      val currentAdapter = viewPager.adapter as? DataCollectionViewPagerAdapter
      if (currentAdapter == null || currentAdapter.tasks != tasks) {
        viewPager.adapter = viewPagerAdapterFactory.create(this, tasks)
      }
    }

    viewModel.currentPosition.observe(viewLifecycleOwner) { viewPager.currentItem = it }
    viewModel.currentTaskDataLiveData.observe(viewLifecycleOwner) {
      viewModel.currentTaskData = it.orElse(null)
    }

    binding.viewModel = viewModel
    binding.lifecycleOwner = this

    getAbstractActivity().setActionBar(binding.dataCollectionToolbar, showTitle = false)

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.findViewById<ConstraintLayout>(R.id.data_collection_container).addView(
      ComposeView(view.context).apply {
        setContent {
          Row(modifier = Modifier.size(200.dp).background(Color.Cyan)) {}
        }
      },
      0
    )
  }

  override fun onBack(): Boolean =
    if (viewPager.currentItem == 0) {
      // If the user is currently looking at the first step, allow the system to handle the
      // Back button. This calls finish() on this activity and pops the back stack.
      false
    } else {
      // Otherwise, select the previous step.
      viewModel.setCurrentPosition(viewModel.currentPosition.value!! - 1)
      true
    }
}
