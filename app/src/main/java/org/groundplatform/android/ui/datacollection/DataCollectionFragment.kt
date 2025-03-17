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
package org.groundplatform.android.ui.datacollection

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.databinding.DataCollectionFragBinding
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener
import org.groundplatform.android.ui.compose.ConfirmationDialog
import org.groundplatform.android.ui.home.HomeScreenFragmentDirections
import org.groundplatform.android.util.renderComposableDialog

/** Fragment allowing the user to collect data to complete a task. */
@AndroidEntryPoint
class DataCollectionFragment : AbstractFragment(), BackPressListener {
  @Inject lateinit var viewPagerAdapterFactory: DataCollectionViewPagerAdapterFactory

  val viewModel: DataCollectionViewModel by hiltNavGraphViewModels(R.id.data_collection)

  private lateinit var binding: DataCollectionFragBinding
  private lateinit var progressBar: ProgressBar
  private lateinit var guideline: Guideline
  private lateinit var viewPager: ViewPager2
  private var isNavigatingUp = false

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

    binding.dataCollectionToolbar.setNavigationOnClickListener { showExitWarningDialog() }

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
            lifecycleScope.launch(Dispatchers.Main) {
              delay(100) // Wait for the keyboard to close
              setProgressBarPosition(view)
            }
          }
        }
      }
    )

    updateUI(
      UiState.TaskListAvailable(
        viewModel.tasks,
        viewModel.getTaskPosition(viewModel.getCurrentTaskId()),
      )
    )

    lifecycleScope.launch { viewModel.uiState.filterNotNull().collect { updateUI(it) } }
  }

  override fun onResume() {
    super.onResume()
    isNavigatingUp = false
  }

  override fun onPause() {
    super.onPause()
    if (!isNavigatingUp) {
      viewModel.saveCurrentState()
    }
  }

  private fun updateUI(uiState: UiState) {
    when (uiState) {
      is UiState.TaskListAvailable -> loadTasks(uiState.tasks, uiState.taskPosition)
      is UiState.TaskUpdated -> onTaskChanged(uiState.taskPosition)
      is UiState.TaskSubmitted -> onTaskSubmitted()
    }
  }

  private fun setProgressBarPosition(view: View) {
    val buttonContainer = view.findViewById<View>(R.id.action_buttons) ?: return

    buttonContainer.doOnLayout {
      val anchorLocation = IntArray(2)
      it.getLocationInWindow(anchorLocation)

      val windowInsets = WindowInsetsCompat.toWindowInsetsCompat(buttonContainer.rootWindowInsets)
      val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

      val guidelineTop = anchorLocation[1] - systemBarsInsets.top

      if (guidelineTop > 0) {
        guideline.setGuidelineBegin(guidelineTop)
      }
    }
  }

  private fun loadTasks(tasks: List<Task>, taskPosition: TaskPosition) {
    val currentAdapter = viewPager.adapter as? DataCollectionViewPagerAdapter
    if (currentAdapter == null || currentAdapter.tasks != tasks) {
      viewPager.adapter = viewPagerAdapterFactory.create(this, tasks)
    }
    viewPager.doOnLayout { onTaskChanged(taskPosition) }
  }

  private fun onTaskChanged(taskPosition: TaskPosition) {
    // Pass false to parameter smoothScroll to avoid smooth scrolling animation.
    viewPager.setCurrentItem(taskPosition.absoluteIndex, false)
    updateProgressBar(taskPosition, true)
  }

  private fun onTaskSubmitted() {
    // Hide close button
    binding.dataCollectionToolbar.navigationIcon = null

    // Display a confirmation dialog and move to home screen after that.
    renderComposableDialog {
      DataSubmissionConfirmationScreen {
        findNavController().navigate(HomeScreenFragmentDirections.showHomeScreen())
      }
    }
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

  override fun onBack(): Boolean {
    if (viewModel.uiState.value == UiState.TaskSubmitted) {
      // Pressing back button after submitting task should navigate back to home screen.
      navigateBack()
    } else if (viewPager.currentItem == 0) {
      showExitWarningDialog()
    } else {
      viewModel.moveToPreviousTask()
    }
    return true
  }

  private fun showExitWarningDialog() {
    renderComposableDialog {
      ConfirmationDialog(
        title = R.string.data_collection_cancellation_title,
        description = R.string.data_collection_cancellation_description,
        confirmButtonText = R.string.data_collection_cancellation_confirm_button,
        onConfirmClicked = { navigateBack() },
      )
    }
  }

  private fun navigateBack() {
    isNavigatingUp = true
    viewModel.clearDraft()
    findNavController().navigateUp()
  }

  private companion object {
    private const val PROGRESS_SCALE = 100
  }
}
