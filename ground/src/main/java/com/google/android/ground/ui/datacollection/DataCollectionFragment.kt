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

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.ground.R
import com.google.android.ground.databinding.DataCollectionFragBinding
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.Navigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

/** Fragment allowing the user to collect data to complete a task. */
@AndroidEntryPoint
class DataCollectionFragment : AbstractFragment(), BackPressListener {
  @Inject lateinit var navigator: Navigator

  @Inject lateinit var viewPagerAdapterFactory: DataCollectionViewPagerAdapterFactory

  private val viewModel: DataCollectionViewModel by hiltNavGraphViewModels(R.id.data_collection)

  private lateinit var binding: DataCollectionFragBinding
  private lateinit var viewPager: ViewPager2

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = DataCollectionFragBinding.inflate(inflater, container, false)
    viewPager = binding.pager
    getAbstractActivity().setSupportActionBar(binding.dataCollectionToolbar)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this

    viewPager.isUserInputEnabled = false
    viewPager.offscreenPageLimit = 1

    loadTasks(viewModel.tasks)
    Timber.e("!!! onViewCreated " + progressBar())
    lifecycleScope.launch { viewModel.currentPosition.collect { onTaskChanged(it) } }

    viewPager.registerOnPageChangeCallback(
      object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)

          val progressBar = progressBar() ?: return
          val currentAdapter = viewPager.adapter as? DataCollectionViewPagerAdapter
          Timber.e("!!! Size ${currentAdapter!!.tasks.size}")
          progressBar.max = (currentAdapter!!.tasks.size - 1) * PROGRESS_SCALE
          progressBar.progress = (viewPager.currentItem - 1) * PROGRESS_SCALE
          progressBar.clearAnimation()

          val progressAnimator =
            ValueAnimator.ofInt(progressBar.progress, viewPager.currentItem * PROGRESS_SCALE)
          progressAnimator.duration = 400L
          progressAnimator.interpolator = FastOutSlowInInterpolator()

          progressAnimator.addUpdateListener { progressBar.progress = it.animatedValue as Int }

          progressAnimator.start()

        }
      }
    )
  }

  private fun loadTasks(tasks: List<Task>) {
    val currentAdapter = viewPager.adapter as? DataCollectionViewPagerAdapter
    if (currentAdapter == null || currentAdapter.tasks != tasks) {
      viewPager.adapter = viewPagerAdapterFactory.create(this, tasks)
    }
  }

  private fun progressBar(): ProgressBar? = requireView().findViewById(R.id.progress_bar)

  private fun onTaskChanged(index: Int) {
    viewPager.currentItem = index
    Timber.e("!!! onTaskChanged  " + progressBar())
  }

  override fun onBack(): Boolean =
    if (viewPager.currentItem == 0) {
      // If the user is currently looking at the first step, allow the system to handle the
      // Back button. This calls finish() on this activity and pops the back stack.
      false
    } else {
      // Otherwise, select the previous step.
      viewModel.updateCurrentPosition(viewModel.getVisibleTaskPosition() - 1)
      true
    }

  private companion object {
    private const val PROGRESS_SCALE = 100
  }
}
