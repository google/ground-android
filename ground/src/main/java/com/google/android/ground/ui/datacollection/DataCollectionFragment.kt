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
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.WindowInsetsCompat
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Fragment allowing the user to collect data to complete a task. */
@AndroidEntryPoint
class DataCollectionFragment : AbstractFragment(), BackPressListener {
  @Inject lateinit var navigator: Navigator
  @Inject lateinit var viewPagerAdapterFactory: DataCollectionViewPagerAdapterFactory

  val viewModel: DataCollectionViewModel by hiltNavGraphViewModels(R.id.data_collection)

  private lateinit var binding: DataCollectionFragBinding
  private lateinit var progressBar: ProgressBar
  private lateinit var guideline: Guideline
  private lateinit var viewPager: ViewPager2

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = DataCollectionFragBinding.inflate(inflater, container, false)
    viewPager = binding.pager
    progressBar = binding.progressBar
    guideline = binding.progressBarGuideline
    getAbstractActivity().setSupportActionBar(binding.dataCollectionToolbar)

    binding.dataCollectionToolbar.setNavigationOnClickListener {
      viewModel.clearDraft()
      navigator.navigateUp()
    }

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this

    viewPager.isUserInputEnabled = false
    viewPager.offscreenPageLimit = 1

    viewPager.registerOnPageChangeCallback(
      object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
          super.onPageScrollStateChanged(state)
          if (state == ViewPager2.SCROLL_STATE_IDLE) {
            Handler(Looper.getMainLooper())
              .postDelayed(
                {
                  // Reset the progress bar position after a delay to wait for the keyboard to
                  // close.
                  setProgressBarPosition(view)
                },
                100,
              )
          }
        }
      }
    )

    lifecycleScope.launch { viewModel.uiState.filterNotNull().collect { updateUI(it) } }
  }

  private fun updateUI(uiState: UiState) {
    when (uiState) {
      is UiState.TaskListAvailable -> loadTasks(uiState.tasks, uiState.taskPosition)
      is UiState.TaskUpdated -> onTaskChanged(uiState.taskPosition)
    }
  }

  private fun setProgressBarPosition(view: View) {
    val buttonContainer = view.findViewById<View>(R.id.action_buttons) ?: return
    val anchorLocation = IntArray(2)
    buttonContainer.getLocationInWindow(anchorLocation)
    val windowInsets = WindowInsetsCompat.toWindowInsetsCompat(buttonContainer.rootWindowInsets)
    val guidelineTop =
      anchorLocation[1] - windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
    guideline.setGuidelineBegin(guidelineTop)
  }

  private fun loadTasks(tasks: List<Task>, taskPosition: TaskPosition) {
    val currentAdapter = viewPager.adapter as? DataCollectionViewPagerAdapter
    if (currentAdapter == null || currentAdapter.tasks != tasks) {
      viewPager.adapter = viewPagerAdapterFactory.create(this, tasks)
    }
    updateProgressBar(taskPosition, false)
  }

  private fun onTaskChanged(taskPosition: TaskPosition) {
    viewPager.currentItem = taskPosition.absoluteIndex
    updateProgressBar(taskPosition, true)
  }

  private fun updateProgressBar(taskPosition: TaskPosition, shouldAnimate: Boolean) {
    // Reset progress bar
    progressBar.max = (taskPosition.sequenceSize - 1) * PROGRESS_SCALE

    if (shouldAnimate) {
      progressBar.clearAnimation()
      with(ValueAnimator.ofInt(progressBar.progress, taskPosition.relativeIndex * PROGRESS_SCALE)) {
        duration = 400L
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener { progressBar.progress = it.animatedValue as Int }
        start()
      }
    } else {
      progressBar.progress = taskPosition.relativeIndex
    }
  }

  override fun onBack(): Boolean =
    if (viewPager.currentItem == 0) {
      // If the user is currently looking at the first step, allow the system to handle the
      // Back button. This calls finish() on this activity and pops the back stack.
      viewModel.clearDraft()
      false
    } else {
      // Otherwise, select the previous step.
      lifecycleScope.launch { viewModel.step(-1) }
      true
    }

  private companion object {
    private const val PROGRESS_SCALE = 100
  }
}
